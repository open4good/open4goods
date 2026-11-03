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

    private static AwinReportClient awin() {
        return new AwinReportClient("100000", "awin-token-value",
                new FakeHttp(request -> response(200, fixture("awin-transactions.json"))).client);
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
    void nominal_emits18EventsWithTheFiguresOfEachNetworkAndTheirTotals() {
        Map<String, MetriksEvent> events = collect(effiliation(), awin());

        assertThat(events).hasSize(18);
        List<String> expectedIds = new java.util.ArrayList<>();
        for (String network : List.of("awin", "effiliation", "total")) {
            for (String window : List.of("7d", "28d")) {
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

        assertThat(events).hasSize(18);
        for (String metric : List.of("commission.validated", "commission.pending", "sales")) {
            for (String window : List.of("7d", "28d")) {
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
