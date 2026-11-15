package org.open4goods.api.services.metriks.affiliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.CLOCK;
import static org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.fixture;
import static org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.response;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.open4goods.api.dto.metriks.MetriksEvent;
import org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.FakeHttp;

/**
 * Tests {@link AffiliationMetriksService}: 7 and 28-day figures of each network from recorded responses, totals,
 * and the error rules (missing credential, failing network, non-EUR amount).
 */
class AffiliationMetriksServiceTest {

    /**
     * The fixture is answered once, to the request that ends the range: Awin is read in several requests (31 days
     * at most each), and the fake HTTP client does not filter by date.
     */
    private static AwinReportClient awin() {
        return new AwinReportClient("100000", "awin-token-value",
                new FakeHttp(request -> response(200, request.uri().getQuery().contains("endDate=2026-10-21T21:59:59")
                        ? fixture("awin-transactions.json") : "[]")).client);
    }

    private static EffiliationReportClient effiliation() {
        return new EffiliationReportClient("effi-key-value",
                new FakeHttp(request -> response(200, fixture("effiliation-transactions.json"))).client);
    }

    /**
     * Commission and sales events only: the click events are covered by {@link AffiliationClickMetriksTest}.
     */
    private static Map<String, MetriksEvent> collect(AffiliationReportClient... clients) {
        List<MetriksEvent> events = new AffiliationMetriksService(List.of(clients), AffiliationTestSupport.noClicks(),
                CLOCK).collectEvents();
        return events.stream().filter(event -> !event.getId().contains(".clicks")).collect(Collectors.toMap(MetriksEvent::getId, Function.identity(),
                (a, b) -> { throw new IllegalStateException("duplicate id " + a.getId()); }, java.util.LinkedHashMap::new));
    }

    private static void assertOk(Map<String, MetriksEvent> events, String id, Object value) {
        MetriksEvent event = events.get(id);
        assertThat(event).as(id).isNotNull();
        assertThat(event.getStatus()).as(id).isEqualTo("ok");
        assertThat(event.getErrorMessage()).as(id).isNull();
        if (value instanceof String decimal) {
            assertThat(event.getValue()).as(id).isEqualTo(new BigDecimal(decimal));
        } else {
            assertThat(event.getValue()).as(id).isEqualTo(value);
        }
    }

    private static void assertError(Map<String, MetriksEvent> events, String id, String message) {
        MetriksEvent event = events.get(id);
        assertThat(event).as(id).isNotNull();
        assertThat(event.getStatus()).as(id).isEqualTo("error");
        assertThat(event.getValue()).as(id).isNull();
        assertThat(event.getErrorMessage()).as(id).isEqualTo(message);
    }

    @Test
    void nominal_emits36EventsWithTheFiguresOfEachNetworkAndTheirTotals() {
        Map<String, MetriksEvent> events = collect(effiliation(), awin());

        assertThat(events).hasSize(36);
        List<String> expectedIds = new java.util.ArrayList<>();
        for (String network : List.of("awin", "effiliation", "total")) {
            for (String window : List.of("7d", "28d", "prev7d", "prev28d")) {
                for (String metric : List.of("commission.validated", "commission.pending", "sales")) {
                    expectedIds.add("affiliation." + network + "." + metric + "." + window);
                }
            }
        }
        assertThat(events.keySet()).containsExactlyElementsOf(expectedIds);
        assertThat(events.keySet()).startsWith("affiliation.awin.commission.validated.7d",
                "affiliation.awin.commission.pending.7d", "affiliation.awin.sales.7d",
                "affiliation.awin.commission.validated.28d");

        // Awin: 7d = 10-20 approved 4.50 + 10-15 (Paris) pending 2.10; 28d adds 10-01 pending 3.00 and 09-24
        // approved 1.25. Declined, deleted and the 09-23 transaction are excluded.
        assertOk(events, "affiliation.awin.commission.validated.7d", "4.50");
        assertOk(events, "affiliation.awin.commission.pending.7d", "2.10");
        assertOk(events, "affiliation.awin.sales.7d", 2L);
        assertOk(events, "affiliation.awin.commission.validated.28d", "5.75");
        assertOk(events, "affiliation.awin.commission.pending.28d", "5.10");
        assertOk(events, "affiliation.awin.sales.28d", 4L);

        assertOk(events, "affiliation.effiliation.commission.validated.7d", "6.00");
        assertOk(events, "affiliation.effiliation.commission.pending.7d", "1.40");
        assertOk(events, "affiliation.effiliation.sales.7d", 2L);
        assertOk(events, "affiliation.effiliation.commission.validated.28d", "14.15");
        assertOk(events, "affiliation.effiliation.commission.pending.28d", "4.00");
        assertOk(events, "affiliation.effiliation.sales.28d", 4L);

        assertOk(events, "affiliation.total.commission.validated.7d", "10.50");
        assertOk(events, "affiliation.total.commission.pending.7d", "3.50");
        assertOk(events, "affiliation.total.sales.7d", 4L);
        assertOk(events, "affiliation.total.commission.validated.28d", "19.90");
        assertOk(events, "affiliation.total.commission.pending.28d", "9.10");
        assertOk(events, "affiliation.total.sales.28d", 8L);
    }

    /**
     * Awin answering from a list of (day, status, commission) transactions, filtered by the bounds of each request
     * like the real API.
     */
    private static AwinReportClient awinWith(String... transactions) {
        return new AwinReportClient("100000", "awin-token-value", new FakeHttp(request -> {
            Map<String, String> query = new java.util.HashMap<>();
            for (String pair : request.uri().getRawQuery().split("&")) {
                String[] kv = pair.split("=");
                query.put(kv[0], java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8));
            }
            StringBuilder body = new StringBuilder("[");
            for (String transaction : transactions) {
                String[] parts = transaction.split(" ");
                String date = parts[0] + "T12:00:00";
                if (date.compareTo(query.get("startDate")) >= 0 && date.compareTo(query.get("endDate")) <= 0) {
                    body.append(body.length() > 1 ? "," : "").append("{\"commissionStatus\":\"").append(parts[1])
                            .append("\",\"transactionDate\":\"").append(date)
                            .append("\",\"commissionAmount\":{\"amount\":").append(parts[2])
                            .append(",\"currency\":\"EUR\"}}");
                }
            }
            return response(200, body.append("]").toString());
        }).client);
    }

    @Test
    void previousWindows_haveTheirOwnBoundsAndParams() {
        Map<String, MetriksEvent> events = collect(awin());

        // Clock: 2026-10-22 (D). 7d = [D-7, D-1], 28d = [D-28, D-1], prev7d = [D-14, D-8], prev28d = [D-56, D-29].
        Map<String, String[]> bounds = Map.of(
                "7d", new String[] {"2026-10-15", "2026-10-21"},
                "28d", new String[] {"2026-09-24", "2026-10-21"},
                "prev7d", new String[] {"2026-10-08", "2026-10-14"},
                "prev28d", new String[] {"2026-08-27", "2026-09-23"});
        bounds.forEach((window, range) -> {
            for (String network : List.of("awin", "total")) {
                MetriksEvent event = events.get("affiliation." + network + ".sales." + window);
                assertThat(event.getParams()).as(window).containsEntry("windowStartDate", range[0])
                        .containsEntry("windowEndDate", range[1]).containsEntry("network", network)
                        .containsEntry("currency", "EUR");
            }
        });
        assertThat(events.get("affiliation.awin.commission.validated.prev7d").getName())
                .isEqualTo("Commissions validées Awin (7 j préc.)");
        assertThat(events.get("affiliation.awin.commission.pending.prev28d").getName())
                .isEqualTo("Commissions en attente Awin (28 j préc.)");
        assertThat(events.get("affiliation.total.sales.prev28d").getName()).isEqualTo("Ventes tous réseaux (28 j préc.)");
    }

    @Test
    void previousWindows_countEachTransactionInItsOwnWindowsOnly() {
        // D-8 (10-14) is in prev7d and 28d, not in 7d; 09-23 (D-29) is in prev28d, not in 28d; 08-26 (D-57) nowhere.
        AwinReportClient awin = awinWith("2026-10-21 approved 1.00", "2026-10-14 approved 2.00",
                "2026-10-08 pending 4.00", "2026-10-07 approved 8.00", "2026-09-23 approved 16.00",
                "2026-08-27 pending 32.00", "2026-08-26 approved 64.00", "2026-10-10 declined 128.00");

        Map<String, MetriksEvent> events = collect(awin);

        assertOk(events, "affiliation.awin.commission.validated.7d", "1.00");
        assertOk(events, "affiliation.awin.commission.pending.7d", "0.00");
        assertOk(events, "affiliation.awin.sales.7d", 1L);
        assertOk(events, "affiliation.awin.commission.validated.28d", "11.00");
        assertOk(events, "affiliation.awin.commission.pending.28d", "4.00");
        assertOk(events, "affiliation.awin.sales.28d", 4L);
        assertOk(events, "affiliation.awin.commission.validated.prev7d", "2.00");
        assertOk(events, "affiliation.awin.commission.pending.prev7d", "4.00");
        assertOk(events, "affiliation.awin.sales.prev7d", 2L);
        assertOk(events, "affiliation.awin.commission.validated.prev28d", "16.00");
        assertOk(events, "affiliation.awin.commission.pending.prev28d", "32.00");
        assertOk(events, "affiliation.awin.sales.prev28d", 2L);
        assertOk(events, "affiliation.total.sales.prev28d", 2L);
    }

    @Test
    void previousWindows_networkReadOnceOverFiftySixDays() {
        FakeHttp http = new FakeHttp(request -> response(200, "{\"data\":[]}"));
        java.util.List<java.time.LocalDate[]> ranges = new java.util.ArrayList<>();
        AffiliationReportClient spy = new EffiliationReportClient("effi-key-value", http.client) {
            @Override
            public List<AffiliationReportTransaction> fetchTransactions(java.time.LocalDate startDate,
                    java.time.LocalDate endDate, java.time.ZoneId zone) {
                ranges.add(new java.time.LocalDate[] {startDate, endDate});
                return List.of();
            }
        };

        collect(spy);

        assertThat(ranges).hasSize(1);
        assertThat(ranges.get(0)[0]).isEqualTo(java.time.LocalDate.of(2026, 8, 27));
        assertThat(ranges.get(0)[1]).isEqualTo(java.time.LocalDate.of(2026, 10, 21));
    }

    @Test
    void failingNetwork_putsItsPreviousWindowsAndTheTotalsInError() {
        EffiliationReportClient serverError = new EffiliationReportClient("effi-key-value",
                new FakeHttp(request -> response(503, "Service Unavailable")).client);

        Map<String, MetriksEvent> events = collect(awinWith("2026-10-14 approved 2.00"), serverError);

        for (String window : List.of("prev7d", "prev28d")) {
            assertError(events, "affiliation.effiliation.sales." + window, "Effiliation : HTTP 503 (conversions)");
            assertError(events, "affiliation.effiliation.commission.validated." + window,
                    "Effiliation : HTTP 503 (conversions)");
            assertError(events, "affiliation.total.commission.pending." + window,
                    "Total indisponible : réseau(x) en erreur : effiliation");
            assertError(events, "affiliation.total.sales." + window,
                    "Total indisponible : réseau(x) en erreur : effiliation");
        }
        assertOk(events, "affiliation.awin.sales.prev7d", 1L);
        assertOk(events, "affiliation.awin.commission.validated.prev7d", "2.00");
    }

    @Test
    void nominal_eventsCarryTheContractMetadata() {
        Map<String, MetriksEvent> events = collect(awin(), effiliation());

        MetriksEvent sevenDays = events.get("affiliation.awin.commission.validated.7d");
        assertThat(sevenDays.getUnit()).isEqualTo("currency");
        assertThat(sevenDays.getGroups()).containsExactly("business", "affiliation");
        assertThat(sevenDays.getTags()).containsExactly("affiliation", "awin");
        assertThat(sevenDays.getParams()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "provider", "open4goods-api",
                "network", "awin",
                "currency", "EUR",
                "windowStartDate", "2026-10-15",
                "windowEndDate", "2026-10-21"));

        MetriksEvent sales = events.get("affiliation.total.sales.28d");
        assertThat(sales.getUnit()).isEqualTo("count");
        assertThat(sales.getTags()).containsExactly("affiliation", "total");
        assertThat(sales.getParams()).containsEntry("windowStartDate", "2026-09-24")
                .containsEntry("windowEndDate", "2026-10-21");
    }

    @Test
    void missingCredential_putsTheNetworkAndTheTotalsInErrorNeverZero() {
        FakeHttp unused = new FakeHttp(request -> response(200, "[]"));
        Map<String, MetriksEvent> events = collect(awin(), new EffiliationReportClient("", unused.client));

        assertThat(events).hasSize(36);
        for (String metric : List.of("commission.validated", "commission.pending", "sales")) {
            for (String window : List.of("7d", "28d", "prev7d", "prev28d")) {
                assertError(events, "affiliation.effiliation." + metric + "." + window,
                        "Credential manquant : metriks.affiliation.effiliation.api-key");
                assertThat(events.get("affiliation.awin." + metric + "." + window).getStatus()).isEqualTo("ok");
                assertError(events, "affiliation.total." + metric + "." + window,
                        "Total indisponible : réseau(x) en erreur : effiliation");
            }
        }
        assertThat(unused.requests).isEmpty();
    }

    @Test
    void missingCredentialOnBothNetworks_listsThemInTheTotalError() {
        FakeHttp unused = new FakeHttp(request -> response(200, "[]"));
        Map<String, MetriksEvent> events = collect(new AwinReportClient("100000", "", unused.client),
                new EffiliationReportClient("", unused.client));

        assertError(events, "affiliation.awin.sales.7d", "Credential manquant : metriks.affiliation.awin.api-token");
        assertError(events, "affiliation.effiliation.sales.7d",
                "Credential manquant : metriks.affiliation.effiliation.api-key");
        assertError(events, "affiliation.total.commission.validated.28d",
                "Total indisponible : réseau(x) en erreur : awin, effiliation");
        assertThat(unused.requests).isEmpty();
    }

    @Test
    void failingNetwork_onlyPutsItsOwnEventsInError() {
        EffiliationReportClient serverError = new EffiliationReportClient("effi-key-value",
                new FakeHttp(request -> response(503, "Service Unavailable")).client);

        Map<String, MetriksEvent> events = collect(awin(), serverError);

        assertOk(events, "affiliation.awin.commission.validated.28d", "5.75");
        assertOk(events, "affiliation.awin.sales.7d", 2L);
        assertError(events, "affiliation.effiliation.sales.28d", "Effiliation : HTTP 503 (conversions)");
        assertError(events, "affiliation.total.commission.validated.7d",
                "Total indisponible : réseau(x) en erreur : effiliation");
    }

    @Test
    void timingOutNetwork_isInErrorAndKeepsTheOtherOne() {
        AwinReportClient timingOut = new AwinReportClient("100000", "awin-token-value",
                new FakeHttp(request -> {
                    throw new HttpTimeoutException("request timed out");
                }).client);

        Map<String, MetriksEvent> events = collect(timingOut, effiliation());

        assertError(events, "affiliation.awin.commission.pending.7d", "Awin : délai dépassé (transactions)");
        assertOk(events, "affiliation.effiliation.sales.28d", 4L);
        assertError(events, "affiliation.total.sales.7d", "Total indisponible : réseau(x) en erreur : awin");
    }

    @Test
    void ioFailure_isReportedWithoutItsMessage() {
        EffiliationReportClient failing = new EffiliationReportClient("effi-key-value",
                new FakeHttp(request -> {
                    throw new IOException("Connection reset");
                }).client);

        Map<String, MetriksEvent> events = collect(failing);

        assertError(events, "affiliation.effiliation.sales.7d",
                "Effiliation : erreur réseau (conversions, IOException)");
    }

    @Test
    void nonEuroAmount_putsTheNetworkInErrorWithoutConversion() {
        String usd = fixture("awin-transactions.json").replaceFirst("\"currency\": \"EUR\"", "\"currency\": \"USD\"");
        AwinReportClient dollars = new AwinReportClient("100000", "awin-token-value",
                new FakeHttp(request -> response(200, usd)).client);

        Map<String, MetriksEvent> events = collect(dollars, effiliation());

        assertError(events, "affiliation.awin.commission.validated.28d",
                "Awin : devise USD non prise en charge (EUR attendu, aucune conversion)");
        assertError(events, "affiliation.total.sales.28d", "Total indisponible : réseau(x) en erreur : awin");
        assertOk(events, "affiliation.effiliation.sales.28d", 4L);
    }

    @Test
    void unexpectedException_reportsItsTypeOnly() {
        AffiliationReportClient broken = new EffiliationReportClient("effi-key-value", null) {
            @Override
            public List<AffiliationReportTransaction> fetchTransactions(java.time.LocalDate startDate,
                    java.time.LocalDate endDate, java.time.ZoneId zone) {
                throw new IllegalStateException("https://api.effinity.fr/apiv3/effi-key-value/publisher/conversions");
            }
        };

        Map<String, MetriksEvent> events = collect(broken);

        assertError(events, "affiliation.effiliation.sales.7d", "Effiliation : erreur inattendue (IllegalStateException)");
    }

    @Test
    void emptyResponses_giveZeroFiguresInOk() {
        AwinReportClient empty = new AwinReportClient("100000", "t", new FakeHttp(request -> response(200, "[]")).client);

        Map<String, MetriksEvent> events = collect(empty);

        assertOk(events, "affiliation.awin.commission.validated.7d", "0.00");
        assertOk(events, "affiliation.awin.sales.28d", 0L);
        assertOk(events, "affiliation.total.sales.28d", 0L);
    }
}
