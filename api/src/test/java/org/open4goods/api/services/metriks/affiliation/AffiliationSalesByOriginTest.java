package org.open4goods.api.services.metriks.affiliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.CLOCK;
import static org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.api.dto.metriks.MetriksEvent;
import org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.FakeClickStats;
import org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.FakeHttp;
import org.open4goods.services.contribution.model.AffiliationClick;
import org.open4goods.services.contribution.model.AffiliationClickStats.OriginClicks;

/**
 * Tests the {@value AffiliationMetriksService#SALES_BY_ORIGIN_ID} event: sales attached to their click through the
 * {@code clickRef} (known, unknown, absent).
 */
class AffiliationSalesByOriginTest {

    private static final String TRANSACTIONS = """
            [
             {"advertiserId":10001,"commissionStatus":"approved","commissionAmount":{"amount":4.5,"currency":"EUR"},
              "clickRefs":{"clickRef":"c-article"},"transactionDate":"2026-10-20T10:00:00"},
             {"advertiserId":10001,"commissionStatus":"pending","commissionAmount":{"amount":2.0,"currency":"EUR"},
              "clickRefs":{"clickRef":"c-article"},"transactionDate":"2026-10-10T10:00:00"},
             {"advertiserId":10001,"commissionStatus":"approved","commissionAmount":{"amount":1.0,"currency":"EUR"},
              "clickRefs":{"clickRef":"c-page"},"transactionDate":"2026-10-12T10:00:00"},
             {"advertiserId":10001,"commissionStatus":"approved","commissionAmount":{"amount":3.0,"currency":"EUR"},
              "clickRefs":{"clickRef":"c-unknown"},"transactionDate":"2026-10-18T11:00:00"},
             {"advertiserId":10001,"commissionStatus":"approved","commissionAmount":{"amount":5.0,"currency":"EUR"},
              "transactionDate":"2026-10-19T11:00:00"},
             {"advertiserId":10001,"commissionStatus":"declined","commissionAmount":{"amount":9.0,"currency":"EUR"},
              "clickRefs":{"clickRef":"c-page"},"transactionDate":"2026-10-20T11:00:00"},
             {"advertiserId":10001,"commissionStatus":"approved","commissionAmount":{"amount":7.0,"currency":"EUR"},
              "clickRefs":{"clickRef":"c-old"},"transactionDate":"2026-09-10T10:00:00"}
            ]""";

    private static AwinReportClient awin(int status) {
        return new AwinReportClient("100000", "awin-token-value", new FakeHttp(request -> response(status,
                request.uri().getPath().endsWith("/programmes") ? "[]"
                        : request.uri().getQuery().contains("endDate=2026-10-21T21:59:59") ? TRANSACTIONS : "[]")).client);
    }

    private static AffiliationClick click(String id, String gtin, String originPath, String contentPath) {
        AffiliationClick click = new AffiliationClick();
        click.setId(id);
        click.setGtin(gtin);
        click.setOriginPath(originPath);
        click.setContentPath(contentPath);
        return click;
    }

    private static FakeClickStats clicks() {
        FakeClickStats stats = AffiliationTestSupport.noClicks();
        stats.byId = Map.of(
                "c-article", click("c-article", "3000000000001", "/blog/x", "/blog/x"),
                "c-page", click("c-page", null, "/produit/y", null),
                "c-old", click("c-old", "3000000000009", "/old", null));
        stats.originClicks = List.of(new OriginClicks("/blog/x", 40), new OriginClicks("/produit/y", 10),
                new OriginClicks("/autre", 5));
        return stats;
    }

    private static MetriksEvent event(AwinReportClient awin, FakeClickStats clicks) {
        return new AffiliationMetriksService(List.of(awin), clicks, CLOCK).collectEvents().stream()
                .filter(e -> e.getId().equals(AffiliationMetriksService.SALES_BY_ORIGIN_ID)).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rows(MetriksEvent event) {
        return (List<Map<String, Object>>) event.getParams().get("rows");
    }

    @Test
    @SuppressWarnings("unchecked")
    void knownClickRef_attachesTheSaleToItsOriginAndProduct() {
        MetriksEvent event = event(awin(200), clicks());

        assertThat(event.getStatus()).isEqualTo("ok");
        Map<String, Object> article = rows(event).get(0);
        assertThat(article).containsEntry("origin", "/blog/x").containsEntry("attributed", true)
                .containsEntry("sales", 2L).containsEntry("commissionValidated", new BigDecimal("4.50"))
                .containsEntry("commissionPending", new BigDecimal("2.00")).containsEntry("clicks", 40L);
        assertThat((List<Object>) article.get("gtins")).containsExactly((Object) Map.of("gtin", "3000000000001", "sales", 2L));
        Map<String, Object> page = rows(event).get(1);
        assertThat(page).containsEntry("origin", "/produit/y").containsEntry("sales", 1L)
                .containsEntry("commissionValidated", new BigDecimal("1.00")).containsEntry("clicks", 10L);
        assertThat((List<?>) page.get("gtins")).isEmpty();
    }

    @Test
    void unknownOrAbsentClickRef_isCountedUnattachedAndNeverSpread() {
        FakeClickStats clicks = clicks();
        MetriksEvent event = event(awin(200), clicks);

        assertThat(rows(event)).hasSize(3);
        Map<String, Object> unattached = rows(event).get(2);
        assertThat(unattached).containsEntry("origin", null).containsEntry("attributed", false)
                .containsEntry("sales", 2L).containsEntry("commissionValidated", new BigDecimal("8.00"))
                .containsEntry("commissionPending", new BigDecimal("0.00")).containsEntry("clicks", null);
        // Declined and out-of-window sales are not looked up nor counted.
        assertThat(clicks.idCalls.get(0)).containsExactlyInAnyOrder("c-article", "c-page", "c-unknown");
    }

    @Test
    void networkInError_isNotMeasuredAndPutsTheEventInError() {
        MetriksEvent event = event(awin(500), clicks());

        assertThat(event.getStatus()).isEqualTo("error");
        assertThat(event.getValue()).isNull();
        assertThat(event.getErrorMessage()).contains("awin");
        assertThat(rows(event).stream().filter(r -> Boolean.TRUE.equals(r.get("attributed")))).isEmpty();
    }

    @Test
    void clicksInError_putsTheEventInErrorWithoutRow() {
        FakeClickStats failing = new FakeClickStats(clicks().read(0, 0, 0, 0, 0), null,
                new IllegalStateException("boom secret-host"));
        MetriksEvent event = event(awin(200), failing);

        assertThat(event.getStatus()).isEqualTo("error");
        assertThat(event.getValue()).isNull();
        assertThat(event.getErrorMessage()).contains("IllegalStateException").doesNotContain("secret-host");
        assertThat(rows(event)).isEmpty();
    }
}
