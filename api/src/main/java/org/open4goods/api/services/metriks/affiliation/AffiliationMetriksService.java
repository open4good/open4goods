package org.open4goods.api.services.metriks.affiliation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.api.dto.metriks.MetriksEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Builds the Metriks affiliation events ({@code GET /api/metriks/affiliation}): validated commissions, pending
 * commissions and sales of each network, plus their totals, over the last 7 and 28 days.
 * <p>
 * Windows: {@code 7d} = [D-7, D-1] and {@code 28d} = [D-28, D-1], D being today in Europe/Paris, by transaction
 * date. Each network is read once, over the 28-day window; the 7-day figures are derived from it.
 * <p>
 * Error rules (a missing figure is never reported as zero):
 * <ul>
 * <li>a network without credentials: its six events are in {@code error}, with
 * {@code Credential manquant : metriks.affiliation.<network>.<property>};</li>
 * <li>an HTTP error, timeout, unreadable response, unknown status or non-EUR amount: only that network's events are
 * in {@code error};</li>
 * <li>a total is in {@code error} as soon as one network is, with the list of the failing networks: never a partial
 * sum.</li>
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
    private static final List<String> NETWORK_ORDER = List.of("awin", "effiliation");
    private static final List<Window> WINDOWS = List.of(new Window("7d", 7, "7 j"), new Window("28d", 28, "28 j"));
    private static final List<String> GROUPS = List.of("business", "affiliation");
    private static final String MASK = "***";

    private static final Logger LOGGER = LoggerFactory.getLogger(AffiliationMetriksService.class);

    private final List<AffiliationReportClient> clients;
    private final Clock clock;

    @Autowired
    public AffiliationMetriksService(List<AffiliationReportClient> clients) {
        this(clients, Clock.system(ZONE));
    }

    /**
     * Package-private constructor allowing tests to fix "today".
     */
    AffiliationMetriksService(List<AffiliationReportClient> clients, Clock clock) {
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
     * {@code commission.validated}, {@code commission.pending} and {@code sales}.
     *
     * @return the 6 events of each network followed by the 6 total events
     */
    public List<MetriksEvent> collectEvents() {
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        LocalDate endDate = today.minusDays(1);
        LocalDate startDate = today.minusDays(WINDOWS.stream().mapToInt(Window::days).max().orElseThrow());

        List<MetriksEvent> events = new ArrayList<>();
        Map<Window, Figures> totals = new LinkedHashMap<>();
        WINDOWS.forEach(window -> totals.put(window, new Figures(BigDecimal.ZERO, BigDecimal.ZERO, 0)));
        List<String> failedNetworks = new ArrayList<>();

        for (AffiliationReportClient client : clients) {
            NetworkResult result = read(client, startDate, endDate);
            for (Window window : WINDOWS) {
                LocalDate windowStart = today.minusDays(window.days());
                Figures figures = result.error() == null ? result.figures().get(window) : null;
                events.addAll(windowEvents(client.network(), client.displayName(), window, windowStart, endDate,
                        figures, result.error()));
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
            events.addAll(windowEvents("total", "tous réseaux", window, today.minusDays(window.days()), endDate,
                    totalError == null ? totals.get(window) : null, totalError));
        }
        return events;
    }

    /**
     * Reads one network over {@code [startDate, endDate]} and aggregates it per window, or returns its error.
     */
    private NetworkResult read(AffiliationReportClient client, LocalDate startDate, LocalDate endDate) {
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
                figures.put(window, aggregate(client, transactions, endDate.plusDays(1).minusDays(window.days()), endDate));
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
                        network, start, end, figures == null ? null : figures.validated().setScale(2, RoundingMode.HALF_UP), error),
                event(prefix + "commission.pending" + suffix, "Commissions en attente" + scope, "currency",
                        network, start, end, figures == null ? null : figures.pending().setScale(2, RoundingMode.HALF_UP), error),
                event(prefix + "sales" + suffix, "Ventes" + scope, "count",
                        network, start, end, figures == null ? null : figures.sales(), error));
    }

    private MetriksEvent event(String id, String name, String unit, String network, LocalDate start, LocalDate end,
                               Number value, String error) {
        MetriksEvent event = new MetriksEvent();
        event.setId(id);
        event.setName(name);
        event.setUnit(unit);
        event.setGroups(GROUPS);
        event.setTags(List.of("affiliation", network));
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("provider", "open4goods-api");
        params.put("network", network);
        params.put("currency", CURRENCY);
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
     * @param key   suffix of the event ids ({@code 7d}, {@code 28d})
     * @param days  number of days, ending yesterday
     * @param label suffix of the event names
     */
    private record Window(String key, int days, String label) {
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
