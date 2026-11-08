package org.open4goods.api.services.metriks.affiliation;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;

/**
 * Reads publisher conversions from the Effinity (ex-Effiliation) API v3
 * ({@code GET https://api.effinity.fr/apiv3/{apikey}/publisher/conversions}), which replaces the API v2 shut down
 * by Effiliation on 11/01/2027 (dd/MM/yyyy).
 * <p>
 * Authentication: the publisher API key (the one of the v2 API, found in the publisher center, tools section),
 * which this API only accepts as a path segment. The request URL therefore carries the secret: it is never
 * logged, put in a message or attached to an exception.
 * Parameters: {@code start} / {@code end} ({@code yyyy-MM-dd}, 90 days at most), {@code dateType=conversionDate}
 * (filter on the conversion date), {@code conversionType=all}, {@code status=all}, {@code page} / {@code perPage}.
 * The pages are read until {@code pagination.lastPage}. The returned {@code conversionDate} is read as a local
 * date-time of the report's time zone (Europe/Paris).
 * <p>
 * Status mapping ({@code status}, case ignored):
 * <ul>
 * <li>{@code validated} &rarr; {@link AffiliationReportStatus#VALIDATED}</li>
 * <li>{@code pending} / {@code reported} (postponed by the merchant, not yet processed) &rarr;
 * {@link AffiliationReportStatus#PENDING}</li>
 * <li>{@code cancelled} &rarr; {@link AffiliationReportStatus#DECLINED}</li>
 * <li>any other value &rarr; error</li>
 * </ul>
 * The commission is {@code commissionAmount}; the {@code currency} field, when present, is reported as is so that
 * a non-EUR amount is rejected rather than summed, and an absent one means euros.
 *
 * @see <a href="https://api.effinity.fr/apiv3/docs">Effinity API v3, OpenAPI description</a>
 */
@Service
public class EffiliationReportClient extends AbstractAffiliationReportClient {

    static final String API_KEY_PROPERTY = "metriks.affiliation.effiliation.api-key";
    static final String CONVERSIONS_URL = "https://api.effinity.fr/apiv3/";
    private static final String STEP = "conversions";
    private static final int PER_PAGE = 1000;
    /** Guard against a pagination that never ends. */
    private static final int MAX_PAGES = 50;
    /** Formats accepted for {@code conversionDate}, the date-time ones first. */
    private static final List<DateTimeFormatter> DATE_TIME_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    private final String apiKey;

    @Autowired
    public EffiliationReportClient(@Value("${" + API_KEY_PROPERTY + ":}") String apiKey) {
        this(apiKey, defaultHttpClient());
    }

    /**
     * Package-private constructor allowing tests to inject a fake {@link HttpClient}.
     */
    EffiliationReportClient(String apiKey, HttpClient httpClient) {
        super(httpClient);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public String network() {
        return "effiliation";
    }

    @Override
    public String displayName() {
        return "Effiliation";
    }

    @Override
    public List<String> missingCredentials() {
        return missing(API_KEY_PROPERTY, apiKey);
    }

    @Override
    public List<String> secretValues() {
        return nonBlank(apiKey);
    }

    @Override
    public List<AffiliationReportTransaction> fetchTransactions(LocalDate startDate, LocalDate endDate, ZoneId zone)
            throws AffiliationReportException {
        List<AffiliationReportTransaction> transactions = new ArrayList<>();
        int lastPage = 1;
        for (int page = 1; page <= lastPage; page++) {
            if (page > MAX_PAGES) {
                throw failure("trop de pages de conversions");
            }
            // The URL carries the key: never log it, never put it in a message.
            URI uri = URI.create(CONVERSIONS_URL + encode(apiKey) + "/publisher/conversions"
                    + "?start=" + startDate
                    + "&end=" + endDate
                    + "&dateType=conversionDate&conversionType=all&status=all"
                    + "&page=" + page + "&perPage=" + PER_PAGE);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            JsonNode root = readJson(send(request, STEP), STEP);
            transactions.addAll(parseTransactions(root));
            lastPage = root.path("pagination").path("lastPage").asInt(1);
        }
        return transactions;
    }

    /**
     * Maps a conversions response ({@code {"pagination": {...}, "conversions": [...]}}) onto normalised
     * transactions.
     */
    List<AffiliationReportTransaction> parseTransactions(JsonNode root) throws AffiliationReportException {
        JsonNode list = root.isArray() ? root : root.path("conversions");
        if (!list.isArray()) {
            throw failure("réponse inattendue (liste de conversions absente)");
        }
        List<AffiliationReportTransaction> transactions = new ArrayList<>();
        for (JsonNode node : list) {
            AffiliationReportStatus status = mapStatus(textOrNull(node.path("status")));
            LocalDate date = parseTransactionDate(textOrNull(node.path("conversionDate")));
            BigDecimal commission = decimalOrNull(node.path("commissionAmount"), "commissionAmount");
            if (status != AffiliationReportStatus.DECLINED && commission == null) {
                throw failure("commission absente");
            }
            String currency = textOrNull(node.path("currency"));
            transactions.add(new AffiliationReportTransaction(date, status, commission, currency == null ? "EUR" : currency));
        }
        return transactions;
    }

    private AffiliationReportStatus mapStatus(String status) throws AffiliationReportException {
        if (status == null) {
            throw failure("status absent");
        }
        String normalized = status.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "validated" -> AffiliationReportStatus.VALIDATED;
            case "pending", "reported" -> AffiliationReportStatus.PENDING;
            case "cancelled", "canceled" -> AffiliationReportStatus.DECLINED;
            default -> throw failure("statut inconnu : " + echo(status));
        };
    }

    private LocalDate parseTransactionDate(String value) throws AffiliationReportException {
        if (value == null) {
            throw failure("conversionDate absent");
        }
        for (DateTimeFormatter format : DATE_TIME_FORMATS) {
            try {
                return LocalDateTime.parse(value, format).toLocalDate();
            } catch (DateTimeParseException e) {
                // try the next format
            }
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            // unreadable
        }
        throw failure("conversionDate illisible : " + echo(value));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
