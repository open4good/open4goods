package org.open4goods.api.services.metriks.affiliation;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
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
 * Reads publisher transactions from the Effiliation reporting API v2
 * ({@code GET https://apiv2.effiliation.com/apiv2/transaction.json}).
 * <p>
 * Authentication: the publisher API key, which this API only accepts in the {@code key} query parameter. The
 * request URL therefore carries the secret: it is never logged, put in a message or attached to an exception.
 * Parameters: {@code start} / {@code end} ({@code dd/MM/yyyy}), {@code type=datetran} (filter on the transaction
 * date), {@code lg=fr}. Dates are French local dates: the window days are passed as is, and the returned
 * {@code datetran} is read as a local date-time of the report's time zone (Europe/Paris).
 * <p>
 * Status mapping ({@code etat}, French or English labels, case and accents ignored):
 * <ul>
 * <li>{@code Valide} / {@code Valid} &rarr; {@link AffiliationReportStatus#VALIDATED}</li>
 * <li>{@code Attente} / {@code Pending} &rarr; {@link AffiliationReportStatus#PENDING}</li>
 * <li>{@code Refus} / {@code Refusal} &rarr; {@link AffiliationReportStatus#DECLINED}</li>
 * <li>any other value &rarr; error</li>
 * </ul>
 * The commission is the {@code commission} field. The API documents no currency field: amounts are those of the
 * publisher account, in euros. A {@code devise} or {@code currency} field, when present, is reported as is so that
 * a non-EUR amount is rejected rather than summed.
 * <p>
 * Effiliation announces the end of API v2 on 11/01/2027 (dd/MM/yyyy) in favour of its API v3 (Effinity): this client will
 * then answer an HTTP error, reported as such by Metriks.
 *
 * @see <a href="https://apiv2.effiliation.com/apiv2/doc/transactions.htm">Effiliation API v2, Transactions</a>
 */
@Service
public class EffiliationReportClient extends AbstractAffiliationReportClient {

    static final String API_KEY_PROPERTY = "metriks.affiliation.effiliation.api-key";
    static final String TRANSACTIONS_URL = "https://apiv2.effiliation.com/apiv2/transaction.json";
    private static final DateTimeFormatter EFFILIATION_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Formats accepted for {@code datetran}, the date-time ones first. */
    private static final List<DateTimeFormatter> DATE_TIME_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            EFFILIATION_DATE);

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
        // The URL carries the key: never log it, never put it in a message.
        URI uri = URI.create(TRANSACTIONS_URL
                + "?key=" + encode(apiKey)
                + "&lg=fr"
                + "&start=" + encode(EFFILIATION_DATE.format(startDate))
                + "&end=" + encode(EFFILIATION_DATE.format(endDate))
                + "&type=datetran");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        JsonNode root = readJson(send(request, "transaction.json"), "transaction.json");
        return parseTransactions(root);
    }

    /**
     * Maps a {@code transaction.json} response ({@code {"transactions": [...]}}) onto normalised transactions.
     */
    List<AffiliationReportTransaction> parseTransactions(JsonNode root) throws AffiliationReportException {
        JsonNode list = root.isArray() ? root : root.path("transactions");
        if (!list.isArray()) {
            throw failure("réponse inattendue (liste de transactions absente)");
        }
        List<AffiliationReportTransaction> transactions = new ArrayList<>();
        for (JsonNode node : list) {
            AffiliationReportStatus status = mapStatus(textOrNull(node.path("etat")));
            LocalDate date = parseTransactionDate(textOrNull(node.path("datetran")));
            BigDecimal commission = decimalOrNull(node.path("commission"), "commission");
            if (status != AffiliationReportStatus.DECLINED && commission == null) {
                throw failure("commission absente");
            }
            String currency = textOrNull(node.path("devise"));
            if (currency == null) {
                currency = textOrNull(node.path("currency"));
            }
            transactions.add(new AffiliationReportTransaction(date, status, commission, currency == null ? "EUR" : currency));
        }
        return transactions;
    }

    private AffiliationReportStatus mapStatus(String etat) throws AffiliationReportException {
        if (etat == null) {
            throw failure("etat absent");
        }
        String normalized = Normalizer.normalize(etat, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "valide", "valid", "validated" -> AffiliationReportStatus.VALIDATED;
            case "attente", "en attente", "pending" -> AffiliationReportStatus.PENDING;
            case "refus", "refuse", "refusal", "refused" -> AffiliationReportStatus.DECLINED;
            default -> throw failure("statut inconnu : " + echo(etat));
        };
    }

    private LocalDate parseTransactionDate(String value) throws AffiliationReportException {
        if (value == null) {
            throw failure("datetran absent");
        }
        for (DateTimeFormatter format : DATE_TIME_FORMATS) {
            try {
                return LocalDateTime.parse(value, format).toLocalDate();
            } catch (DateTimeParseException e) {
                // try the next format
            }
        }
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, format);
            } catch (DateTimeParseException e) {
                // try the next format
            }
        }
        throw failure("datetran illisible : " + echo(value));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
