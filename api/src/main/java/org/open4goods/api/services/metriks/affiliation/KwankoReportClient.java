package org.open4goods.api.services.metriks.affiliation;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
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
 * Reads publisher conversions from the Kwanko Publisher API
 * ({@code GET https://api.kwanko.com/publishers/conversions}).
 * <p>
 * Authentication: the publisher token, sent as a bearer {@code Authorization} header, never in the URL. By default it
 * is the token of the Kwanko feed ({@code affiliation-config.kwanko-token}). Parameters:
 * {@code completion_date_from} / {@code completion_date_to}, ISO date-times with an explicit {@code +00:00} offset
 * (Kwanko rejects the {@code Z} suffix). The window days, expressed in the report's time zone, are converted to UTC
 * bounds, and each {@code completed_at} is converted back to that zone.
 * <p>
 * Pagination: when the response is an object carrying a {@code pagination} node with a total page count
 * ({@code total_pages} or {@code last_page}), the following pages are read with {@code page=N}. Without it, the
 * answer is a single page.
 * <p>
 * Status mapping ({@code state}, case-insensitive):
 * <ul>
 * <li>{@code approved}, {@code validated}, {@code accepted} &rarr; {@link AffiliationReportStatus#VALIDATED}</li>
 * <li>{@code pending}, {@code waiting}, {@code ongoing} &rarr; {@link AffiliationReportStatus#PENDING}</li>
 * <li>{@code rejected}, {@code declined}, {@code refused}, {@code cancelled}, {@code canceled} &rarr;
 * {@link AffiliationReportStatus#DECLINED}</li>
 * <li>any other value &rarr; error</li>
 * </ul>
 * The commission is {@code earnings.value} (or {@code spending.value}, or the first website's), in
 * {@code campaign.currency} (or {@code countervalue_currency}); same fields as {@code KwankoFeedService}.
 */
@Service
public class KwankoReportClient extends AbstractAffiliationReportClient {

    static final String API_TOKEN_PROPERTY = "metriks.affiliation.kwanko.api-token";
    static final String CONVERSIONS_URL = "https://api.kwanko.com/publishers/conversions";
    /** Safety cap on the number of pages read. */
    static final int MAX_PAGES = 50;
    private static final DateTimeFormatter KWANKO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    private final String apiToken;

    @Autowired
    public KwankoReportClient(@Value("${" + API_TOKEN_PROPERTY + ":}") String apiToken) {
        this(apiToken, defaultHttpClient());
    }

    /**
     * Package-private constructor allowing tests to inject a fake {@link HttpClient}.
     */
    KwankoReportClient(String apiToken, HttpClient httpClient) {
        super(httpClient);
        this.apiToken = apiToken == null ? "" : apiToken.trim();
    }

    @Override
    public String network() {
        return "kwanko";
    }

    @Override
    public String displayName() {
        return "Kwanko";
    }

    @Override
    public List<String> missingCredentials() {
        return missing(API_TOKEN_PROPERTY, apiToken);
    }

    @Override
    public List<String> secretValues() {
        return nonBlank(apiToken);
    }

    @Override
    public List<AffiliationReportTransaction> fetchTransactions(LocalDate startDate, LocalDate endDate, ZoneId zone)
            throws AffiliationReportException {
        Instant from = startDate.atStartOfDay(zone).toInstant();
        Instant to = endDate.plusDays(1).atStartOfDay(zone).toInstant().minusSeconds(1);
        String base = CONVERSIONS_URL
                + "?completion_date_from=" + encode(KWANKO_DATE_TIME.format(from.atOffset(ZoneOffset.UTC)))
                + "&completion_date_to=" + encode(KWANKO_DATE_TIME.format(to.atOffset(ZoneOffset.UTC)));

        List<AffiliationReportTransaction> transactions = new ArrayList<>();
        int totalPages = 1;
        for (int page = 1; page <= totalPages; page++) {
            if (page > MAX_PAGES) {
                throw failure("trop de pages (plus de " + MAX_PAGES + ")");
            }
            URI uri = URI.create(page == 1 ? base : base + "&page=" + page);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            JsonNode root = readJson(send(request, "conversions"), "conversions");
            transactions.addAll(parseTransactions(root, zone));
            totalPages = Math.max(totalPages, totalPages(root));
        }
        return transactions;
    }

    private static int totalPages(JsonNode root) {
        JsonNode pagination = root.path("pagination");
        for (String field : new String[] {"total_pages", "last_page"}) {
            JsonNode node = pagination.path(field);
            if (node.isNumber()) {
                return node.asInt(1);
            }
        }
        return 1;
    }

    /**
     * Maps a Kwanko conversions response (an array, or an object with a {@code conversions}, {@code data} or
     * {@code items} array) onto normalised transactions.
     */
    List<AffiliationReportTransaction> parseTransactions(JsonNode root, ZoneId zone) throws AffiliationReportException {
        JsonNode conversions = conversionsNode(root);
        if (conversions == null) {
            throw failure("réponse inattendue (tableau de conversions attendu)");
        }
        List<AffiliationReportTransaction> transactions = new ArrayList<>();
        for (JsonNode node : conversions) {
            AffiliationReportStatus status = mapStatus(textOrNull(node.path("state")));
            LocalDate date = parseCompletedAt(textOrNull(node.path("completed_at")), zone);
            JsonNode earnings = firstNonEmpty(node.path("earnings"), node.path("spending"),
                    node.path("websites_per_language").path(0).path("earnings"),
                    node.path("websites_per_language").path(0).path("spending"));
            BigDecimal amount = decimalOrNull(firstPresent(earnings.path("value"), earnings.path("countervalue")),
                    "earnings.value");
            String currency = textOrNull(node.path("campaign").path("currency"));
            if (currency == null) {
                currency = textOrNull(node.path("countervalue_currency"));
            }
            if (status != AffiliationReportStatus.DECLINED && (amount == null || currency == null)) {
                throw failure("commission incomplète (montant ou devise absent)");
            }
            transactions.add(new AffiliationReportTransaction(date, status, amount, currency));
        }
        return transactions;
    }

    private static JsonNode conversionsNode(JsonNode root) {
        if (root.isArray()) {
            return root;
        }
        for (String field : new String[] {"conversions", "data", "items"}) {
            if (root.path(field).isArray()) {
                return root.path(field);
            }
        }
        return null;
    }

    private static JsonNode firstNonEmpty(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (!node.isMissingNode() && !node.isNull() && !node.isEmpty()) {
                return node;
            }
        }
        return nodes[0];
    }

    private static JsonNode firstPresent(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (textOrNull(node) != null) {
                return node;
            }
        }
        return nodes[0];
    }

    private AffiliationReportStatus mapStatus(String state) throws AffiliationReportException {
        if (state == null) {
            throw failure("state absent");
        }
        return switch (state.toLowerCase(Locale.ROOT)) {
            case "approved", "validated", "accepted" -> AffiliationReportStatus.VALIDATED;
            case "pending", "waiting", "ongoing" -> AffiliationReportStatus.PENDING;
            case "rejected", "declined", "refused", "cancelled", "canceled" -> AffiliationReportStatus.DECLINED;
            default -> throw failure("statut inconnu : " + echo(state));
        };
    }

    private LocalDate parseCompletedAt(String value, ZoneId zone) throws AffiliationReportException {
        if (value == null) {
            throw failure("completed_at absent");
        }
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(zone).toLocalDate();
        } catch (DateTimeParseException e) {
            throw failure("completed_at illisible : " + echo(value));
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
