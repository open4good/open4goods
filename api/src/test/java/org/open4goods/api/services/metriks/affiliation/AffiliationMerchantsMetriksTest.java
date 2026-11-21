package org.open4goods.api.services.metriks.affiliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.CLOCK;
import static org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.open4goods.api.dto.metriks.MetriksEvent;
import org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.FakeClickStats;
import org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.FakeHttp;
import org.open4goods.services.contribution.model.AffiliationClickStats.MerchantClicks;

/**
 * Tests the {@value AffiliationMetriksService#MERCHANTS_SALES_ID} event: sales, commissions and clicks per merchant
 * over the four windows ({@code 7d} = [10-15, 10-21], {@code 28d} = [09-24, 10-21], {@code prev7d} = [10-08, 10-14],
 * {@code prev28d} = [08-27, 09-23]).
 */
class AffiliationMerchantsMetriksTest {

    private static final String AWIN_TRANSACTIONS = """
            [
             {"advertiserId":10001,"commissionStatus":"approved","commissionAmount":{"amount":4.5,"currency":"EUR"},
              "clickRefs":{"clickRef":"c1"},"transactionDate":"2026-10-20T10:00:00"},
             {"advertiserId":10001,"commissionStatus":"pending","commissionAmount":{"amount":2.0,"currency":"EUR"},
              "transactionDate":"2026-10-10T10:00:00"},
             {"advertiserId":10001,"commissionStatus":"approved","commissionAmount":{"amount":1.0,"currency":"EUR"},
              "transactionDate":"2026-09-10T10:00:00"},
             {"advertiserId":10001,"commissionStatus":"declined","commissionAmount":{"amount":9.0,"currency":"EUR"},
              "transactionDate":"2026-10-20T11:00:00"},
             {"advertiserId":10003,"commissionStatus":"approved","commissionAmount":{"amount":3.0,"currency":"EUR"},
              "transactionDate":"2026-10-18T11:00:00"}
            ]""";
    private static final String PROGRAMMES = "[{\"id\":10001,\"name\":\"Marchand A\"},{\"id\":10003,\"name\":\"Réseau seul\"}]";

    private static final MerchantDatasourceResolver RESOLVER = new MerchantDatasourceResolver() {
        @Override
        public Optional<String> datasourceOf(String network, String advertiserId) {
            return "awin".equals(network) && "10001".equals(advertiserId) ? Optional.of("marchand-a.fr") : Optional.empty();
        }

        @Override
        public boolean hasProgramme(String network, String datasource) {
            return "awin".equals(network) && ("marchand-a.fr".equals(datasource) || "marchand-b.fr".equals(datasource));
        }
    };

    private static AwinReportClient awin(int status) {
        return new AwinReportClient("100000", "awin-token-value", new FakeHttp(request -> response(
                status == 200 && request.uri().getPath().endsWith("/programmes") ? 200 : status,
                request.uri().getPath().endsWith("/programmes") ? PROGRAMMES
                        : request.uri().getQuery().contains("endDate=2026-10-21T21:59:59") ? AWIN_TRANSACTIONS : "[]")).client);
    }

    /** Clicks by window start: 7d 10-15, 28d 09-24, prev7d 10-08, prev28d 08-27 (Paris midnight, in epoch millis). */
    private static long millis(String date) {
        return LocalDate.parse(date).atStartOfDay(AffiliationMetriksService.ZONE).toInstant().toEpochMilli();
    }

    private static FakeClickStats clicks() {
        FakeClickStats stats = AffiliationTestSupport.noClicks();
        stats.merchantClicks = (start, end) -> {
            if (start == millis("2026-10-15")) {
                return List.of(new MerchantClicks("marchand-a.fr", "awin", 20), new MerchantClicks("marchand-b.fr", "awin", 4),
                        new MerchantClicks("amazon.fr", "amazon", 6));
            }
            if (start == millis("2026-09-24")) {
                return List.of(new MerchantClicks("marchand-a.fr", "awin", 50), new MerchantClicks("marchand-b.fr", "awin", 10),
                        new MerchantClicks("amazon.fr", "amazon", 12), new MerchantClicks("kwanko-shop.fr", "kwanko", 5));
            }
            if (start == millis("2026-10-08")) {
                return List.of(new MerchantClicks("marchand-a.fr", "awin", 10));
            }
            return List.of(new MerchantClicks("marchand-a.fr", "awin", 40));
        };
        return stats;
    }

    private static MetriksEvent merchants(AwinReportClient awin, FakeClickStats clicks) {
        return new AffiliationMetriksService(List.of(awin), clicks, CLOCK, RESOLVER).collectEvents().stream()
                .filter(event -> event.getId().equals(AffiliationMetriksService.MERCHANTS_SALES_ID)).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rows(MetriksEvent event) {
        return (List<Map<String, Object>>) event.getParams().get("rows");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> window(Map<String, Object> row, String key) {
        return (Map<String, Object>) row.get(key);
    }

    private static Map<String, Object> row(MetriksEvent event, String merchant) {
        return rows(event).stream().filter(row -> merchant.equals(row.get("merchant"))).findFirst().orElseThrow();
    }

    @Test
    void merchantWithSalesAndClicks_hasExactFiguresOnTheFourWindows() {
        MetriksEvent event = merchants(awin(200), clicks());

        assertThat(event.getStatus()).isEqualTo("ok");
        assertThat(event.getValue()).isEqualTo(5);
        Map<String, Object> a = row(event, "marchand-a.fr");
        assertThat(a).containsEntry("network", "awin").containsEntry("advertiserId", "10001")
                .containsEntry("datasource", true).containsEntry("salesMeasured", true).containsEntry("clicksMeasured", true);
        // 7d: one approved sale of 4.50 (the declined one is excluded), 20 clicks.
        assertThat(window(a, "7d")).containsEntry("sales", 1L).containsEntry("commissionValidated", new java.math.BigDecimal("4.50"))
                .containsEntry("commissionPending", new java.math.BigDecimal("0.00")).containsEntry("clicks", 20L)
                .containsEntry("salesPerClick", new java.math.BigDecimal("0.0500"));
        // 28d: the 10-10 pending sale is added; 50 clicks.
        assertThat(window(a, "28d")).containsEntry("sales", 2L).containsEntry("commissionValidated", new java.math.BigDecimal("4.50"))
                .containsEntry("commissionPending", new java.math.BigDecimal("2.00")).containsEntry("clicks", 50L)
                .containsEntry("salesPerClick", new java.math.BigDecimal("0.0400"));
        // prev7d: the 10-10 pending sale only, 10 clicks.
        assertThat(window(a, "prev7d")).containsEntry("sales", 1L).containsEntry("commissionPending", new java.math.BigDecimal("2.00"))
                .containsEntry("commissionValidated", new java.math.BigDecimal("0.00")).containsEntry("clicks", 10L)
                .containsEntry("salesPerClick", new java.math.BigDecimal("0.1000"));
        // prev28d: the 09-10 approved sale, 40 clicks.
        assertThat(window(a, "prev28d")).containsEntry("sales", 1L).containsEntry("commissionValidated", new java.math.BigDecimal("1.00"))
                .containsEntry("clicks", 40L).containsEntry("salesPerClick", new java.math.BigDecimal("0.0250"));
    }

    @Test
    void merchantWithClicksOnly_hasAMeasuredZeroSaleAndNoRatio() {
        MetriksEvent event = merchants(awin(200), clicks());

        Map<String, Object> b = row(event, "marchand-b.fr");
        assertThat(b).containsEntry("salesMeasured", true).containsEntry("clicksMeasured", true).containsEntry("advertiserId", null);
        assertThat(window(b, "7d")).containsEntry("sales", 0L).containsEntry("commissionValidated", new java.math.BigDecimal("0.00"))
                .containsEntry("clicks", 4L).containsEntry("salesPerClick", new java.math.BigDecimal("0.0000"));
        // Not clicked in a previous window: a measured 0 click, so no ratio.
        assertThat(window(b, "prev7d")).containsEntry("clicks", 0L).containsEntry("salesPerClick", null);
    }

    @Test
    void advertiserWithoutDatasource_keepsItsNetworkNameAndItsClicksAreNotMeasured() {
        MetriksEvent event = merchants(awin(200), clicks());

        Map<String, Object> shop = row(event, "Réseau seul");
        assertThat(shop).containsEntry("datasource", false).containsEntry("salesMeasured", true).containsEntry("clicksMeasured", false)
                .containsEntry("advertiserId", "10003");
        assertThat(window(shop, "28d")).containsEntry("sales", 1L).containsEntry("clicks", null).containsEntry("salesPerClick", null);
    }

    @Test
    void networksWithoutSalesReading_haveSalesNotMeasuredNeverZero() {
        MetriksEvent event = merchants(awin(200), clicks());

        for (String merchant : List.of("amazon.fr", "kwanko-shop.fr")) {
            Map<String, Object> row = row(event, merchant);
            assertThat(row).containsEntry("salesMeasured", false).containsEntry("clicksMeasured", true);
            assertThat(window(row, "28d")).containsEntry("sales", null).containsEntry("commissionValidated", null)
                    .containsEntry("commissionPending", null).containsEntry("salesPerClick", null);
        }
        assertThat(window(row(event, "amazon.fr"), "28d")).containsEntry("clicks", 12L);
    }

    @Test
    void rowsAreSortedBySalesThenClicks_andMerchantsWithoutActivityAreOmitted() {
        MetriksEvent event = merchants(awin(200), clicks());

        assertThat(rows(event).stream().map(row -> row.get("merchant")).toList())
                .containsExactly("marchand-a.fr", "Réseau seul", "amazon.fr", "marchand-b.fr", "kwanko-shop.fr")
                .doesNotContain((Object) null);
        assertThat(rows(event)).hasSize(5);
    }

    @Test
    void networkInError_putsItsRowInErrorAndTheEventInError() {
        MetriksEvent event = merchants(awin(503), clicks());

        assertThat(event.getStatus()).isEqualTo("error");
        assertThat(event.getValue()).isNull();
        assertThat(event.getErrorMessage()).contains("awin").doesNotContain("awin-token-value");
        Map<String, Object> failed = rows(event).get(0);
        assertThat(failed).containsEntry("network", "awin").containsEntry("status", "error").containsEntry("merchant", null);
        assertThat((String) failed.get("error")).contains("HTTP 503").doesNotContain("awin-token-value");
        // No partial row of the failing network: its click-only merchants are not listed with figures.
        assertThat(rows(event).stream().filter(row -> "awin".equals(row.get("network")))).hasSize(1);
        // The other networks stay readable.
        assertThat(row(event, "amazon.fr")).containsEntry("status", "ok");
    }

    @Test
    void clicksInError_putsTheEventInErrorWithoutRows() {
        MetriksEvent event = merchants(awin(200), new FakeClickStats(null, new IllegalStateException("boom")));

        assertThat(event.getStatus()).isEqualTo("error");
        assertThat(event.getValue()).isNull();
        assertThat(event.getErrorMessage()).contains("Clics indisponibles");
        assertThat(rows(event)).isEmpty();
    }

    @Test
    void withoutAnyDatasourceMatch_everySaleStaysOnItsNetworkName() {
        MetriksEvent event = new AffiliationMetriksService(List.of(awin(200)), clicks(), CLOCK).collectEvents().stream()
                .filter(e -> e.getId().equals(AffiliationMetriksService.MERCHANTS_SALES_ID)).findFirst().orElseThrow();

        assertThat(row(event, "Marchand A")).containsEntry("datasource", false).containsEntry("clicksMeasured", false);
        // marchand-b.fr has clicks but no known programme: its sales are not measured.
        assertThat(row(event, "marchand-b.fr")).containsEntry("salesMeasured", false);
    }
}
