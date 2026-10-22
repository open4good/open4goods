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

    private static RakutenReportClient rakuten() {
        return new RakutenReportClient("rk-client-id", "rk-client-secret", "1234567", new FakeHttp(
                request -> request.uri().getPath().equals("/token")
                        ? response(200, "{\"access_token\":\"rk-access-value\"}")
                        : response(200, fixture("rakuten-events.json"))).client);
    }

    private static Map<String, MetriksEvent> collect(AffiliationReportClient... clients) {
        List<MetriksEvent> events = new AffiliationMetriksService(List.of(clients), CLOCK).collectEvents();
        return events.stream().collect(Collectors.toMap(MetriksEvent::getId, Function.identity(),
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
    void nominal_emits24EventsWithTheFiguresOfEachNetworkAndTheirTotals() {
        Map<String, MetriksEvent> events = collect(rakuten(), awin(), effiliation());

        assertThat(events).hasSize(24);
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

        assertOk(events, "affiliation.rakuten.commission.validated.7d", "1.00");
        assertOk(events, "affiliation.rakuten.commission.pending.7d", "0.80");
        assertOk(events, "affiliation.rakuten.sales.7d", 2L);
        assertOk(events, "affiliation.rakuten.commission.validated.28d", "4.30");
        assertOk(events, "affiliation.rakuten.commission.pending.28d", "1.30");
        assertOk(events, "affiliation.rakuten.sales.28d", 4L);

        assertOk(events, "affiliation.total.commission.validated.7d", "11.50");
        assertOk(events, "affiliation.total.commission.pending.7d", "4.30");
        assertOk(events, "affiliation.total.sales.7d", 6L);
        assertOk(events, "affiliation.total.commission.validated.28d", "24.20");
        assertOk(events, "affiliation.total.commission.pending.28d", "10.40");
        assertOk(events, "affiliation.total.sales.28d", 12L);
    }

    @Test
    void nominal_eventsCarryTheContractMetadata() {
        Map<String, MetriksEvent> events = collect(awin(), effiliation(), rakuten());

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
        Map<String, MetriksEvent> events = collect(
                new AwinReportClient("100000", "", unused.client), effiliation(),
                new RakutenReportClient("", "", "", unused.client));

        for (String metric : List.of("commission.validated", "commission.pending", "sales")) {
            for (String window : List.of("7d", "28d")) {
                assertError(events, "affiliation.awin." + metric + "." + window,
                        "Credential manquant : metriks.affiliation.awin.api-token");
                assertError(events, "affiliation.rakuten." + metric + "." + window,
                        "Credential manquant : metriks.affiliation.rakuten.client-id, "
                                + "metriks.affiliation.rakuten.client-secret, metriks.affiliation.rakuten.sid");
                assertThat(events.get("affiliation.effiliation." + metric + "." + window).getStatus()).isEqualTo("ok");
                assertError(events, "affiliation.total." + metric + "." + window,
                        "Total indisponible : réseau(x) en erreur : awin, rakuten");
            }
        }
        assertThat(unused.requests).isEmpty();
    }

    @Test
    void failingNetwork_onlyPutsItsOwnEventsInError() {
        RakutenReportClient timingOut = new RakutenReportClient("id", "secret", "1234567",
                new FakeHttp(request -> {
                    throw new HttpTimeoutException("request timed out");
                }).client);
        EffiliationReportClient serverError = new EffiliationReportClient("effi-key-value",
                new FakeHttp(request -> response(503, "Service Unavailable")).client);

        Map<String, MetriksEvent> events = collect(awin(), serverError, timingOut);

        assertOk(events, "affiliation.awin.commission.validated.28d", "5.75");
        assertOk(events, "affiliation.awin.sales.7d", 2L);
        assertError(events, "affiliation.effiliation.sales.28d", "Effiliation : HTTP 503 (conversions)");
        assertError(events, "affiliation.rakuten.commission.pending.7d", "Rakuten : délai dépassé (jeton OAuth)");
        assertError(events, "affiliation.total.commission.validated.7d",
                "Total indisponible : réseau(x) en erreur : effiliation, rakuten");
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

        Map<String, MetriksEvent> events = collect(dollars, effiliation(), rakuten());

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
