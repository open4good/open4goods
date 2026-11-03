package org.open4goods.api.services.metriks.affiliation;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
 * Reads publisher transactions from the Awin Publisher API
 * ({@code GET https://api.awin.com/publishers/{publisherId}/transactions/}).
 * <p>
 * Authentication: the publisher's API token (Awin UI, "API Credentials"), sent as an OAuth2 bearer
 * {@code Authorization} header, never in the URL. Parameters: {@code startDate} / {@code endDate}
 * ({@code yyyy-MM-ddTHH:mm:ss}, at most 31 days apart), {@code timezone=UTC}, {@code dateType=transaction}. The
 * window days, expressed in the report's time zone, are converted to UTC bounds, and each returned
 * {@code transactionDate} (UTC) is converted back to that zone.
 * <p>
 * Status mapping ({@code commissionStatus}):
 * <ul>
 * <li>{@code approved} &rarr; {@link AffiliationReportStatus#VALIDATED}</li>
 * <li>{@code pending} &rarr; {@link AffiliationReportStatus#PENDING}</li>
 * <li>{@code declined}, {@code deleted} &rarr; {@link AffiliationReportStatus#DECLINED}</li>
 * <li>any other value &rarr; error</li>
 * </ul>
 * The commission is {@code commissionAmount.amount}, in {@code commissionAmount.currency}.
 *
 * @see <a href="https://help.awin.com/apidocs/returns-a-list-of-transactions-for-a-given-publisher">Awin
 *      Publisher API, transactions</a>
 */
@Service
public class AwinReportClient extends AbstractAffiliationReportClient {

    static final String PUBLISHER_ID_PROPERTY = "metriks.affiliation.awin.publisher-id";
    static final String API_TOKEN_PROPERTY = "metriks.affiliation.awin.api-token";
    static final String TRANSACTIONS_URL = "https://api.awin.com/publishers/%s/transactions/";
    private static final DateTimeFormatter AWIN_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final String publisherId;
    private final String apiToken;

    @Autowired
    public AwinReportClient(@Value("${" + PUBLISHER_ID_PROPERTY + ":}") String publisherId,
                            @Value("${" + API_TOKEN_PROPERTY + ":}") String apiToken) {
        this(publisherId, apiToken, defaultHttpClient());
    }

    /**
     * Package-private constructor allowing tests to inject a fake {@link HttpClient}.
     */
    AwinReportClient(String publisherId, String apiToken, HttpClient httpClient) {
        super(httpClient);
        this.publisherId = publisherId == null ? "" : publisherId.trim();
        this.apiToken = apiToken == null ? "" : apiToken.trim();
    }

    @Override
    public String network() {
        return "awin";
    }

    @Override
    public String displayName() {
        return "Awin";
    }

    @Override
    public List<String> missingCredentials() {
        return missing(PUBLISHER_ID_PROPERTY, publisherId, API_TOKEN_PROPERTY, apiToken);
    }

    @Override
    public List<String> secretValues() {
        return nonBlank(apiToken);
    }

    @Override
    public List<AffiliationReportTransaction> fetchTransactions(LocalDate startDate, LocalDate endDate, ZoneId zone)
            throws AffiliationReportException {
        if (!publisherId.matches("\\d+")) {
            throw failure(PUBLISHER_ID_PROPERTY + " doit être numérique");
        }
        Instant from = startDate.atStartOfDay(zone).toInstant();
        Instant to = endDate.plusDays(1).atStartOfDay(zone).toInstant().minusSeconds(1);

        URI uri = URI.create(String.format(TRANSACTIONS_URL, publisherId)
                + "?startDate=" + encode(AWIN_DATE_TIME.format(from.atOffset(ZoneOffset.UTC)))
                + "&endDate=" + encode(AWIN_DATE_TIME.format(to.atOffset(ZoneOffset.UTC)))
                + "&timezone=UTC&dateType=transaction");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + apiToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        JsonNode root = readJson(send(request, "transactions"), "transactions");
        return parseTransactions(root, zone);
    }

    /**
     * Maps an Awin transactions response (a JSON array) onto normalised transactions.
     */
    List<AffiliationReportTransaction> parseTransactions(JsonNode root, ZoneId zone) throws AffiliationReportException {
        if (!root.isArray()) {
            throw failure("réponse inattendue (tableau de transactions attendu)");
        }
        List<AffiliationReportTransaction> transactions = new ArrayList<>();
        for (JsonNode node : root) {
            AffiliationReportStatus status = mapStatus(textOrNull(node.path("commissionStatus")));
            LocalDate date = parseTransactionDate(textOrNull(node.path("transactionDate")), zone);
            JsonNode commission = node.path("commissionAmount");
            BigDecimal amount = decimalOrNull(commission.path("amount"), "commissionAmount.amount");
            String currency = textOrNull(commission.path("currency"));
            if (status != AffiliationReportStatus.DECLINED && (amount == null || currency == null)) {
                throw failure("commissionAmount incomplet (montant ou devise absent)");
            }
            transactions.add(new AffiliationReportTransaction(date, status, amount, currency));
        }
        return transactions;
    }

    private AffiliationReportStatus mapStatus(String status) throws AffiliationReportException {
        if (status == null) {
            throw failure("commissionStatus absent");
        }
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "approved" -> AffiliationReportStatus.VALIDATED;
            case "pending" -> AffiliationReportStatus.PENDING;
            case "declined", "deleted" -> AffiliationReportStatus.DECLINED;
            default -> throw failure("statut inconnu : " + echo(status));
        };
    }

    /**
     * Parses {@code transactionDate}: an ISO date-time with an offset, or without one (then UTC, the requested
     * time zone), and returns its day in {@code zone}.
     */
    private LocalDate parseTransactionDate(String value, ZoneId zone) throws AffiliationReportException {
        if (value == null) {
            throw failure("transactionDate absent");
        }
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(zone).toLocalDate();
        } catch (DateTimeParseException e) {
            // No offset: the API answered in the requested time zone (UTC).
        }
        try {
            return LocalDateTime.parse(value).atOffset(ZoneOffset.UTC).atZoneSameInstant(zone).toLocalDate();
        } catch (DateTimeParseException e) {
            throw failure("transactionDate illisible : " + echo(value));
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
