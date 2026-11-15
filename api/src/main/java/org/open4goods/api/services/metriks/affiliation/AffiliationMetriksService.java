package org.open4goods.api.services.metriks.affiliation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.api.dto.metriks.MetriksEvent;
import org.open4goods.services.contribution.model.AffiliationClick;
import org.open4goods.services.contribution.model.AffiliationClickStats;
import org.open4goods.services.contribution.model.AffiliationClickStats.ClickCounts;
import org.open4goods.services.contribution.model.AffiliationClickStats.MerchantClicks;
import org.open4goods.services.contribution.model.AffiliationClickStats.OriginClicks;
import org.open4goods.services.contribution.model.AffiliationNetwork;
import org.open4goods.services.contribution.service.AffiliationClickStatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.stereotype.Service;

/**
 * Builds the Metriks affiliation events ({@code GET /api/metriks/affiliation}): validated commissions, pending
 * commissions and sales of each network, plus their totals, over the last 7 and 28 days and over the 7 and 28 days
 * before them ({@code prev7d}, {@code prev28d}); then the outbound
 * {@code /contrib/} clicks per network ({@code awin}, {@code effiliation}, {@code kwanko}, {@code tradetracker}, {@code amazon}, {@code other}) and in total over the same
 * windows, the 20 most clicked merchants and the 50 most clicked origins (article or guide, otherwise originating
 * page) over 28 days.
 * <p>
 * Windows: {@code 7d} = [D-7, D-1], {@code 28d} = [D-28, D-1], {@code prev7d} = [D-14, D-8] and {@code prev28d} =
 * [D-56, D-29], D being today in Europe/Paris, by transaction date (commissions) or click time (clicks). Each network
 * is read once, over the 56 days [D-56, D-1] (the client splits the request when its API limits the range, e.g.
 * Awin: 31 days); the four windows are derived from it. Clicks are counted on the {@code affiliation-clicks} index
 * written by front-api: one aggregation for the current windows and the tops, one per previous window.
 * <p>
 * Events: 12 per network (3 metrics x 4 windows) and 12 totals, then 28 click events (7 scopes x 4 windows) and the
 * 2 tops.
 * <p>
 * Error rules (a missing figure is never reported as zero):
 * <ul>
 * <li>a network without credentials: its twelve events are in {@code error}, with
 * {@code Credential manquant : metriks.affiliation.<network>.<property>};</li>
 * <li>an HTTP error, timeout, unreadable response, unknown status or non-EUR amount: only that network's events are
 * in {@code error};</li>
 * <li>a total is in {@code error} as soon as one network is, with the list of the failing networks: never a partial
 * sum;</li>
 * <li>clicks: when Elasticsearch cannot answer (unreachable, missing index...), the thirty click events (tops included) are in
 * {@code error}; a network without any click is a real {@code 0}.</li>
 * </ul>
 * Error messages and logs never carry a credential: client messages are built without secrets, and every message
 * is additionally scrubbed of the configured credential values.
 */
@Service
public class AffiliationMetriksService {

    /** Time zone of the report windows. */
    public static final ZoneId ZONE = ZoneId.of("Europe/Paris");
    /** Only currency summed: no conversion. */
    static final String CURRENCY = "EUR";
    private static final List<String> NETWORK_ORDER = List.of("awin", "effiliation", "kwanko", "tradetracker");
    /** Click networks, in event order ({@link AffiliationNetwork#key()}). */
    private static final List<String> CLICK_NETWORKS = Arrays.stream(AffiliationNetwork.values())
            .map(AffiliationNetwork::key).toList();
    static final String TOP_MERCHANTS_ID = "affiliation.clicks.top_merchants.28d";
    static final String TOP_ORIGINS_ID = "affiliation.clicks.top_origins.28d";
    private static final Window SHORT = new Window("7d", 7, 0, "7 j");
    private static final Window LONG = new Window("28d", 28, 0, "28 j");
    private static final Window PREV_SHORT = new Window("prev7d", 7, 7, "7 j préc.");
    private static final Window PREV_LONG = new Window("prev28d", 28, 28, "28 j préc.");
    private static final List<Window> WINDOWS = List.of(SHORT, LONG, PREV_SHORT, PREV_LONG);
    private static final List<String> GROUPS = List.of("business", "affiliation");
    private static final String MASK = "***";

    private static final Logger LOGGER = LoggerFactory.getLogger(AffiliationMetriksService.class);

    private final List<AffiliationReportClient> clients;
    private final AffiliationClickStatsService clickStatsService;
    private final Clock clock;

    @Autowired
    public AffiliationMetriksService(List<AffiliationReportClient> clients,
                                     AffiliationClickStatsService clickStatsService) {
        this(clients, clickStatsService, Clock.system(ZONE));
    }

    /**
     * Package-private constructor allowing tests to fix "today".
     */
    AffiliationMetriksService(List<AffiliationReportClient> clients, AffiliationClickStatsService clickStatsService,
                              Clock clock) {
        this.clickStatsService = clickStatsService;
        this.clients = clients.stream()
                .sorted(Comparator.comparingInt(c -> {
                    int index = NETWORK_ORDER.indexOf(c.network());
                    return index < 0 ? NETWORK_ORDER.size() : index;
                }))
                .toList();
        this.clock = clock;
    }

    /**
     * Reads every network and builds the events: for each network then for the totals, and for each window,
     * {@code commission.validated}, {@code commission.pending} and {@code sales}; then the click events.
     *
     * @return the 6 events of each network, the 6 total events, then the 16 click events
     *         ({@link #clickEvents(LocalDate)})
     */
    public List<MetriksEvent> collectEvents() {
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        LocalDate endDate = today.minusDays(1);
        LocalDate startDate = WINDOWS.stream().map(window -> window.start(today)).min(Comparator.naturalOrder())
                .orElseThrow();

        List<MetriksEvent> events = new ArrayList<>();
        Map<Window, Figures> totals = new LinkedHashMap<>();
        WINDOWS.forEach(window -> totals.put(window, new Figures(BigDecimal.ZERO, BigDecimal.ZERO, 0)));
        List<String> failedNetworks = new ArrayList<>();

        for (AffiliationReportClient client : clients) {
            NetworkResult result = read(client, today, startDate, endDate);
            for (Window window : WINDOWS) {
                Figures figures = result.error() == null ? result.figures().get(window) : null;
                events.addAll(windowEvents(client.network(), client.displayName(), window, window.start(today),
                        window.end(today), figures, result.error()));
                if (figures != null) {
                    totals.put(window, totals.get(window).plus(figures));
                }
            }
            if (result.error() != null) {
                failedNetworks.add(client.network());
            }
        }

        String totalError = failedNetworks.isEmpty() ? null
                : "Total indisponible : réseau(x) en erreur : " + String.join(", ", failedNetworks);
        for (Window window : WINDOWS) {
            events.addAll(windowEvents("total", "tous réseaux", window, window.start(today), window.end(today),
                    totalError == null ? totals.get(window) : null, totalError));
        }
        events.addAll(clickEvents(today));
        return events;
    }

    /**
     * Builds the click events: {@code affiliation.<network>.clicks.<window>} for each {@link AffiliationNetwork} (in enum order,
     * {@code other} last) then {@code total}, each for {@code 7d}, {@code 28d}, {@code prev7d} then {@code prev28d},
     * followed by {@value #TOP_MERCHANTS_ID} and {@value #TOP_ORIGINS_ID}. All of them are in {@code error} when
     * Elasticsearch cannot answer one of the three requests.
     */
    private List<MetriksEvent> clickEvents(LocalDate today) {
        LocalDate endDate = today.minusDays(1);
        long end = today.atStartOfDay(ZONE).toInstant().toEpochMilli();
        Window shortWindow = SHORT;
        Window longWindow = LONG;
        AffiliationClickStats stats = null;
        ClickCounts prevShort = null;
        ClickCounts prevLong = null;
        String error = null;
        try {
            stats = clickStatsService.read(
                    today.minusDays(longWindow.days()).atStartOfDay(ZONE).toInstant().toEpochMilli(),
                    today.minusDays(shortWindow.days()).atStartOfDay(ZONE).toInstant().toEpochMilli(),
                    end, AffiliationClickStatsService.TOP_MERCHANTS, AffiliationClickStatsService.TOP_ORIGINS);
            prevShort = clickStatsService.count(startMillis(PREV_SHORT, today), endMillis(PREV_SHORT, today));
            prevLong = clickStatsService.count(startMillis(PREV_LONG, today), endMillis(PREV_LONG, today));
        } catch (NoSuchIndexException e) {
            error = "Clics indisponibles : index " + AffiliationClick.INDEX_NAME + " absent";
        } catch (RuntimeException e) {
            // The exception message is not trusted to be secret-free (cluster address, credentials): only its type.
            error = "Clics indisponibles : Elasticsearch en erreur (" + e.getClass().getSimpleName() + ")";
        }
        if (error != null) {
            // A failure after the first read (previous windows) must not leave current figures next to the error.
            stats = null;
            LOGGER.warn("Metriks affiliation clicks en erreur : {}", error);
        }

        List<MetriksEvent> events = new ArrayList<>();
        List<String> networks = new ArrayList<>(CLICK_NETWORKS);
        networks.add("total");
        for (String network : networks) {
            for (Window window : WINDOWS) {
                Long value = null;
                if (stats != null) {
                    Map<String, Long> perNetwork;
                    long total;
                    if (window.equals(SHORT)) {
                        perNetwork = stats.clicksShort();
                        total = stats.totalShort();
                    } else if (window.equals(LONG)) {
                        perNetwork = stats.clicksLong();
                        total = stats.totalLong();
                    } else {
                        ClickCounts counts = window.equals(PREV_SHORT) ? prevShort : prevLong;
                        perNetwork = counts.clicks();
                        total = counts.total();
                    }
                    value = "total".equals(network) ? total : perNetwork.getOrDefault(network, 0L);
                }
                String label = "total".equals(network) ? "tous réseaux" : labelOf(network);
                events.add(event("affiliation." + network + ".clicks." + window.key(),
                        "Clics sortants " + label + " (" + window.label() + ")", "count", network,
                        window.start(today), window.end(today), null, value, error));
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        if (stats != null) {
            for (MerchantClicks merchant : stats.topMerchantsLong()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("merchant", merchant.merchant());
                row.put("network", merchant.network());
                row.put("clicks", merchant.clicks());
                rows.add(row);
            }
        }
        MetriksEvent top = event(TOP_MERCHANTS_ID,
                "Top " + AffiliationClickStatsService.TOP_MERCHANTS + " marchands par clics sortants ("
                        + longWindow.label() + ")",
                "count", "total", today.minusDays(longWindow.days()), endDate, null,
                stats == null ? null : rows.size(), error);
        if (stats != null) {
            top.getParams().put("rows", rows);
        }
        events.add(top);

        List<Map<String, Object>> originRows = new ArrayList<>();
        if (stats != null) {
            for (OriginClicks origin : stats.topOriginsLong()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("origin", origin.origin());
                row.put("clicks", origin.clicks());
                originRows.add(row);
            }
        }
        MetriksEvent origins = event(TOP_ORIGINS_ID,
                "Top " + AffiliationClickStatsService.TOP_ORIGINS + " pages d'origine par clics sortants ("
                        + longWindow.label() + ")",
                "count", "total", today.minusDays(longWindow.days()), endDate, null,
                stats == null ? null : originRows.size(), error);
        if (stats != null) {
            origins.getParams().put("rows", originRows);
        }
        events.add(origins);
        return events;
    }

    private static long startMillis(Window window, LocalDate today) {
        return window.start(today).atStartOfDay(ZONE).toInstant().toEpochMilli();
    }

    /** Exclusive end of the window: midnight following its last day. */
    private static long endMillis(Window window, LocalDate today) {
        return window.end(today).plusDays(1).atStartOfDay(ZONE).toInstant().toEpochMilli();
    }

    private static String labelOf(String key) {
        return Arrays.stream(AffiliationNetwork.values()).filter(n -> n.key().equals(key)).findFirst()
                .map(AffiliationNetwork::label).orElse(key);
    }

    /**
     * Reads one network over {@code [startDate, endDate]} and aggregates it per window, or returns its error.
     */
    private NetworkResult read(AffiliationReportClient client, LocalDate today, LocalDate startDate,
                               LocalDate endDate) {
        List<String> missing = client.missingCredentials();
        if (!missing.isEmpty()) {
            String message = "Credential manquant : " + String.join(", ", missing);
            LOGGER.warn("Metriks affiliation {}: {}", client.network(), message);
            return NetworkResult.failed(message);
        }
        try {
            List<AffiliationReportTransaction> transactions = client.fetchTransactions(startDate, endDate, ZONE);
            Map<Window, Figures> figures = new LinkedHashMap<>();
            for (Window window : WINDOWS) {
                figures.put(window, aggregate(client, transactions, window.start(today), window.end(today)));
            }
            return new NetworkResult(figures, null);
        } catch (AffiliationReportException e) {
            String message = scrub(e.getMessage(), client.secretValues());
            LOGGER.warn("Metriks affiliation {} en erreur : {}", client.network(), message);
            return NetworkResult.failed(message);
        } catch (RuntimeException e) {
            // Unexpected failure: its message is not trusted to be secret-free, only its type is reported.
            String message = client.displayName() + " : erreur inattendue (" + e.getClass().getSimpleName() + ")";
            LOGGER.warn("Metriks affiliation {} en erreur : {}", client.network(), message);
            return NetworkResult.failed(message);
        }
    }

    /**
     * Sums the validated and pending commissions, and counts the validated and pending sales, of the transactions
     * dated in {@code [start, end]}.
     *
     * @throws AffiliationReportException when a counted transaction is not in EUR
     */
    private Figures aggregate(AffiliationReportClient client, List<AffiliationReportTransaction> transactions,
                              LocalDate start, LocalDate end) throws AffiliationReportException {
        BigDecimal validated = BigDecimal.ZERO;
        BigDecimal pending = BigDecimal.ZERO;
        long sales = 0;
        for (AffiliationReportTransaction transaction : transactions) {
            LocalDate date = transaction.transactionDate();
            if (transaction.status() == AffiliationReportStatus.DECLINED || date.isBefore(start) || date.isAfter(end)) {
                continue;
            }
            if (!CURRENCY.equalsIgnoreCase(transaction.currency())) {
                throw new AffiliationReportException(client.displayName() + " : devise "
                        + AbstractAffiliationReportClient.echo(transaction.currency())
                        + " non prise en charge (EUR attendu, aucune conversion)");
            }
            if (transaction.status() == AffiliationReportStatus.VALIDATED) {
                validated = validated.add(transaction.commission());
            } else {
                pending = pending.add(transaction.commission());
            }
            sales++;
        }
        return new Figures(validated, pending, sales);
    }

    /**
     * Builds the three events of one network (or of the totals) for one window.
     */
    private List<MetriksEvent> windowEvents(String network, String label, Window window, LocalDate start,
                                            LocalDate end, Figures figures, String error) {
        String prefix = "affiliation." + network + ".";
        String suffix = "." + window.key();
        String scope = " " + label + " (" + window.label() + ")";
        return List.of(
                event(prefix + "commission.validated" + suffix, "Commissions validées" + scope, "currency",
                        network, start, end, CURRENCY, figures == null ? null : figures.validated().setScale(2, RoundingMode.HALF_UP), error),
                event(prefix + "commission.pending" + suffix, "Commissions en attente" + scope, "currency",
                        network, start, end, CURRENCY, figures == null ? null : figures.pending().setScale(2, RoundingMode.HALF_UP), error),
                event(prefix + "sales" + suffix, "Ventes" + scope, "count",
                        network, start, end, CURRENCY, figures == null ? null : figures.sales(), error));
    }

    /**
     * Builds one event; {@code currency} is omitted from {@code params} when {@code null} (click events).
     */
    private MetriksEvent event(String id, String name, String unit, String network, LocalDate start, LocalDate end,
                               String currency, Number value, String error) {
        MetriksEvent event = new MetriksEvent();
        event.setId(id);
        event.setName(name);
        event.setUnit(unit);
        event.setGroups(GROUPS);
        event.setTags(List.of("affiliation", network));
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("provider", "open4goods-api");
        params.put("network", network);
        if (currency != null) {
            params.put("currency", currency);
        }
        params.put("windowStartDate", start.toString());
        params.put("windowEndDate", end.toString());
        event.setParams(params);
        if (error != null) {
            event.setStatus("error");
            event.setValue(null);
            event.setErrorMessage(error);
        } else {
            event.setStatus("ok");
            event.setValue(value);
        }
        return event;
    }

    /**
     * Replaces every configured credential value found in {@code message} with {@value #MASK}.
     */
    static String scrub(String message, List<String> secrets) {
        if (message == null) {
            return "erreur sans message";
        }
        String scrubbed = message;
        for (String secret : secrets) {
            if (secret != null && !secret.isBlank()) {
                scrubbed = scrubbed.replace(secret, MASK);
            }
        }
        return scrubbed;
    }

    /**
     * A report window.
     *
     * @param key        suffix of the event ids ({@code 7d}, {@code 28d}, {@code prev7d}, {@code prev28d})
     * @param days       number of days
     * @param offsetDays number of days between the end of the window and yesterday (0 for the current windows)
     * @param label      suffix of the event names
     */
    private record Window(String key, int days, int offsetDays, String label) {
        /** First day: D-offset-days, D being today. */
        LocalDate start(LocalDate today) {
            return today.minusDays(offsetDays + days);
        }

        /** Last day, inclusive: D-offset-1. */
        LocalDate end(LocalDate today) {
            return today.minusDays(offsetDays + 1L);
        }
    }

    /**
     * Aggregated figures of one window.
     */
    private record Figures(BigDecimal validated, BigDecimal pending, long sales) {
        Figures plus(Figures other) {
            return new Figures(validated.add(other.validated), pending.add(other.pending), sales + other.sales);
        }
    }

    /**
     * Figures per window of one network, or its error message.
     */
    private record NetworkResult(Map<Window, Figures> figures, String error) {
        static NetworkResult failed(String error) {
            return new NetworkResult(Map.of(), error);
        }
    }
}
