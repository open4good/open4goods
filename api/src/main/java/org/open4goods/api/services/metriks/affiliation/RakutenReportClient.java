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
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;

/**
 * Reads publisher transactions from the Rakuten Advertising Events API
 * ({@code GET https://api.linksynergy.com/events/1.0/transactions}).
 * <p>
 * Authentication: an OAuth access token obtained from {@code POST https://api.linksynergy.com/token}, with the
 * header {@code Authorization: Bearer base64(clientId:clientSecret)} (the "token key" of the Developer Portal)
 * and the form body {@code scope=<SID>} (the publisher site id). The access token is then sent as a bearer
 * {@code Authorization} header. Parameters: {@code transaction_date_start} / {@code transaction_date_end}
 * ({@code yyyy-MM-dd HH:mm:ss}, GMT), {@code limit} and {@code page}. The window days, expressed in the report's
 * time zone, are converted to GMT bounds, and each returned {@code transaction_date} (GMT) back to that zone.
 * <p>
 * The Events API reports no approval status. Its records are either real-time <em>events</em>
 * ({@code is_event=Y}: a sale was seen, the advertiser may still drop it) or <em>transactions</em> reported in
 * batch by the advertiser ({@code is_event=N}); a cancellation is a batch record with a negative amount. Records
 * are grouped by {@code order_id}, and each order maps to one normalised transaction:
 * <ul>
 * <li>the order has batch records whose commissions sum to more than zero &rarr;
 * {@link AffiliationReportStatus#VALIDATED}, with that net commission (real-time events of the order are
 * superseded and ignored)</li>
 * <li>the order has batch records whose commissions sum to zero or less (fully cancelled) &rarr;
 * {@link AffiliationReportStatus#DECLINED}</li>
 * <li>the order only has real-time events &rarr; {@link AffiliationReportStatus#PENDING}, with the sum of their
 * commissions</li>
 * <li>an {@code is_event} value other than {@code Y} / {@code N} &rarr; error</li>
 * </ul>
 * The transaction date of an order is the earliest {@code transaction_date} of its records; every record of an
 * order must carry the same {@code currency}.
 *
 * @see <a href="https://pubhelp.rakutenadvertising.com/hc/en-us/articles/5949867433997-Events-API">Rakuten
 *      Advertising, Events API</a>
 */
@Service
public class RakutenReportClient extends AbstractAffiliationReportClient {

    static final String CLIENT_ID_PROPERTY = "metriks.affiliation.rakuten.client-id";
    static final String CLIENT_SECRET_PROPERTY = "metriks.affiliation.rakuten.client-secret";
    static final String SID_PROPERTY = "metriks.affiliation.rakuten.sid";
    static final String TOKEN_URL = "https://api.linksynergy.com/token";
    static final String EVENTS_URL = "https://api.linksynergy.com/events/1.0/transactions";
    /** Page size requested from the Events API (its default). */
    static final int PAGE_SIZE = 100;
    /** Safety cap on the number of pages read for one window. */
    static final int MAX_PAGES = 100;
    private static final DateTimeFormatter RAKUTEN_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** {@code Wed Oct 01 2026 10:15:00 GMT+0000}, optionally followed by a zone name in parentheses. */
    private static final Pattern TEXTUAL_DATE = Pattern.compile(
            "^(?:[A-Za-z]{3},? )?([A-Za-z]{3}) (\\d{1,2}) (\\d{4}) (\\d{2}):(\\d{2}):(\\d{2})"
                    + "(?: (?:GMT|UTC)?([+-]\\d{2}:?\\d{2}))?.*$");

    private final String clientId;
    private final String clientSecret;
    private final String sid;

    @Autowired
    public RakutenReportClient(@Value("${" + CLIENT_ID_PROPERTY + ":}") String clientId,
                               @Value("${" + CLIENT_SECRET_PROPERTY + ":}") String clientSecret,
                               @Value("${" + SID_PROPERTY + ":}") String sid) {
        this(clientId, clientSecret, sid, defaultHttpClient());
    }

    /**
     * Package-private constructor allowing tests to inject a fake {@link HttpClient}.
     */
    RakutenReportClient(String clientId, String clientSecret, String sid, HttpClient httpClient) {
        super(httpClient);
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
        this.sid = sid == null ? "" : sid.trim();
    }

    @Override
    public String network() {
        return "rakuten";
    }

    @Override
    public String displayName() {
        return "Rakuten";
    }

    @Override
    public List<String> missingCredentials() {
        return missing(CLIENT_ID_PROPERTY, clientId, CLIENT_SECRET_PROPERTY, clientSecret, SID_PROPERTY, sid);
    }

    @Override
    public List<String> secretValues() {
        return nonBlank(clientId, clientSecret, tokenKey());
    }

    @Override
    public List<AffiliationReportTransaction> fetchTransactions(LocalDate startDate, LocalDate endDate, ZoneId zone)
            throws AffiliationReportException {
        if (!sid.matches("\\d+")) {
            throw failure(SID_PROPERTY + " doit être numérique");
        }
        String accessToken = accessToken();

        Instant from = startDate.atStartOfDay(zone).toInstant();
        Instant to = endDate.plusDays(1).atStartOfDay(zone).toInstant().minusSeconds(1);
        String query = "?transaction_date_start=" + encode(RAKUTEN_DATE_TIME.format(from.atOffset(ZoneOffset.UTC)))
                + "&transaction_date_end=" + encode(RAKUTEN_DATE_TIME.format(to.atOffset(ZoneOffset.UTC)))
                + "&limit=" + PAGE_SIZE;

        List<JsonNode> records = new ArrayList<>();
        for (int page = 1; ; page++) {
            if (page > MAX_PAGES) {
                throw failure("plus de " + MAX_PAGES + " pages de transactions, lecture interrompue");
            }
            HttpRequest request = HttpRequest.newBuilder(URI.create(EVENTS_URL + query + "&page=" + page))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            JsonNode root = readJson(send(request, "events/transactions"), "events/transactions");
            if (!root.isArray()) {
                throw failure("réponse inattendue (tableau de transactions attendu)");
            }
            root.forEach(records::add);
            if (root.size() < PAGE_SIZE) {
                break;
            }
        }
        return parseTransactions(records, zone);
    }

    /**
     * Exchanges the client id, client secret and SID for an access token.
     */
    private String accessToken() throws AffiliationReportException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + tokenKey())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("scope=" + encode(sid)))
                .build();
        JsonNode root = readJson(send(request, "jeton OAuth"), "jeton OAuth");
        String token = textOrNull(root.path("access_token"));
        if (token == null) {
            throw failure("jeton OAuth absent de la réponse");
        }
        return token;
    }

    /**
     * Maps Events API records onto one normalised transaction per order (see the class Javadoc).
     */
    List<AffiliationReportTransaction> parseTransactions(List<JsonNode> records, ZoneId zone)
            throws AffiliationReportException {
        Map<String, Order> orders = new LinkedHashMap<>();
        int anonymous = 0;
        for (JsonNode node : records) {
            String orderId = textOrNull(node.path("order_id"));
            if (orderId == null) {
                // Without an order id a record cannot be grouped: it stands alone.
                orderId = "\u0000" + anonymous++;
            }
            boolean event = parseIsEvent(textOrNull(node.path("is_event")));
            BigDecimal commission = decimalOrNull(node.path("commissions"), "commissions");
            if (commission == null) {
                throw failure("commissions absent");
            }
            String currency = textOrNull(node.path("currency"));
            if (currency == null) {
                throw failure("currency absent");
            }
            LocalDate date = parseTransactionDate(textOrNull(node.path("transaction_date")), zone);
            Order order = orders.computeIfAbsent(orderId, id -> new Order());
            order.add(event, commission, currency.toUpperCase(Locale.ROOT), date);
            if (order.mixedCurrencies) {
                throw failure("devises différentes dans une même commande");
            }
        }

        List<AffiliationReportTransaction> transactions = new ArrayList<>();
        for (Order order : orders.values()) {
            transactions.add(order.toTransaction());
        }
        return transactions;
    }

    private boolean parseIsEvent(String value) throws AffiliationReportException {
        if (value == null) {
            throw failure("is_event absent");
        }
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "Y", "TRUE" -> true;
            case "N", "FALSE" -> false;
            default -> throw failure("is_event inconnu : " + echo(value));
        };
    }

    /**
     * Parses {@code transaction_date}: {@code EEE MMM dd yyyy HH:mm:ss GMT+0000}, an ISO date-time with or without
     * offset, or {@code yyyy-MM-dd HH:mm:ss}; values without offset are GMT. Returns the day in {@code zone}.
     */
    private LocalDate parseTransactionDate(String value, ZoneId zone) throws AffiliationReportException {
        if (value == null) {
            throw failure("transaction_date absent");
        }
        Matcher matcher = TEXTUAL_DATE.matcher(value);
        try {
            if (matcher.matches()) {
                String offset = matcher.group(7) == null ? "+0000" : matcher.group(7).replace(":", "");
                String normalized = String.format("%s %02d %s %s:%s:%s %s", matcher.group(1),
                        Integer.parseInt(matcher.group(2)), matcher.group(3), matcher.group(4), matcher.group(5),
                        matcher.group(6), offset);
                return OffsetDateTime.parse(normalized,
                        DateTimeFormatter.ofPattern("MMM dd yyyy HH:mm:ss xx", Locale.ENGLISH))
                        .atZoneSameInstant(zone).toLocalDate();
            }
            if (value.contains("T")) {
                try {
                    return OffsetDateTime.parse(value).atZoneSameInstant(zone).toLocalDate();
                } catch (DateTimeParseException e) {
                    return LocalDateTime.parse(value).atOffset(ZoneOffset.UTC).atZoneSameInstant(zone).toLocalDate();
                }
            }
            return LocalDateTime.parse(value, RAKUTEN_DATE_TIME).atOffset(ZoneOffset.UTC)
                    .atZoneSameInstant(zone).toLocalDate();
        } catch (DateTimeParseException | NumberFormatException e) {
            throw failure("transaction_date illisible : " + echo(value));
        }
    }

    /**
     * @return the "token key" {@code base64(clientId:clientSecret)}, or an empty string when unconfigured
     */
    private String tokenKey() {
        if (clientId.isEmpty() || clientSecret.isEmpty()) {
            return "";
        }
        return Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * URL-encodes a value, spaces as {@code %20} (the dates carry one).
     */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * Records of one order, split between real-time events and batch transactions.
     */
    private static final class Order {
        private BigDecimal eventCommission = BigDecimal.ZERO;
        private BigDecimal batchCommission = BigDecimal.ZERO;
        private boolean hasBatch;
        private String currency;
        private boolean mixedCurrencies;
        private LocalDate date;

        void add(boolean event, BigDecimal commission, String recordCurrency, LocalDate recordDate) {
            if (event) {
                eventCommission = eventCommission.add(commission);
            } else {
                hasBatch = true;
                batchCommission = batchCommission.add(commission);
            }
            if (currency == null) {
                currency = recordCurrency;
            } else if (!currency.equals(recordCurrency)) {
                mixedCurrencies = true;
            }
            if (date == null || recordDate.isBefore(date)) {
                date = recordDate;
            }
        }

        AffiliationReportTransaction toTransaction() {
            if (!hasBatch) {
                return new AffiliationReportTransaction(date, AffiliationReportStatus.PENDING, eventCommission, currency);
            }
            if (batchCommission.signum() > 0) {
                return new AffiliationReportTransaction(date, AffiliationReportStatus.VALIDATED, batchCommission, currency);
            }
            return new AffiliationReportTransaction(date, AffiliationReportStatus.DECLINED, batchCommission, currency);
        }
    }
}
