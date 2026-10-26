package org.open4goods.api.services.metriks.affiliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.CLOCK;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.open4goods.api.dto.metriks.MetriksEvent;
import org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.FakeClickStats;
import org.open4goods.services.contribution.model.AffiliationClickStats;
import org.open4goods.services.contribution.model.AffiliationClickStats.MerchantClicks;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Tests the click events of {@link AffiliationMetriksService}: ids and order, values per network and window, window
 * bounds sent to Elasticsearch (Europe/Paris days), top merchants, and errors (never zero).
 */
class AffiliationClickMetriksTest {

    private static final List<String> CLICK_IDS = List.of(
            "affiliation.awin.clicks.7d", "affiliation.awin.clicks.28d",
            "affiliation.effiliation.clicks.7d", "affiliation.effiliation.clicks.28d",
            "affiliation.other.clicks.7d", "affiliation.other.clicks.28d",
            "affiliation.total.clicks.7d", "affiliation.total.clicks.28d",
            "affiliation.clicks.top_merchants.28d");

    private static final AffiliationClickStats STATS = new AffiliationClickStats(
            Map.of("awin", 3L, "effiliation", 0L, "other", 1L),
            Map.of("awin", 10L, "effiliation", 4L, "other", 2L),
            4, 16,
            List.of(new MerchantClicks("darty.com", "awin", 7), new MerchantClicks("boulanger.com", "effiliation", 4)));

    private static List<MetriksEvent> collectAll(FakeClickStats clicks) {
        // No commission network: only the 6 (error-free, zero) totals precede the click events.
        return new AffiliationMetriksService(List.of(), clicks, CLOCK).collectEvents();
    }

    private static Map<String, MetriksEvent> clickEvents(FakeClickStats clicks) {
        return collectAll(clicks).stream()
                .filter(event -> event.getId().contains(".clicks"))
                .collect(Collectors.toMap(MetriksEvent::getId, Function.identity(),
                        (a, b) -> { throw new IllegalStateException("duplicate id " + a.getId()); },
                        LinkedHashMap::new));
    }

    @Test
    void nominal_emitsTheNineClickEventsAfterTheCommissionEvents() {
        List<MetriksEvent> all = collectAll(new FakeClickStats(STATS, null));

        List<String> ids = all.stream().map(MetriksEvent::getId).toList();
        assertThat(ids.subList(ids.size() - CLICK_IDS.size(), ids.size())).containsExactlyElementsOf(CLICK_IDS);
        assertThat(ids.subList(0, ids.size() - CLICK_IDS.size())).allMatch(id -> !id.contains(".clicks"));
    }

    @Test
    void nominal_reportsTheClicksOfEachNetworkAndWindowZeroBeingAValue() {
        Map<String, MetriksEvent> events = clickEvents(new FakeClickStats(STATS, null));

        Map<String, Long> expected = Map.of(
                "affiliation.awin.clicks.7d", 3L, "affiliation.awin.clicks.28d", 10L,
                "affiliation.effiliation.clicks.7d", 0L, "affiliation.effiliation.clicks.28d", 4L,
                "affiliation.other.clicks.7d", 1L, "affiliation.other.clicks.28d", 2L,
                "affiliation.total.clicks.7d", 4L, "affiliation.total.clicks.28d", 16L);
        expected.forEach((id, value) -> {
            MetriksEvent event = events.get(id);
            assertThat(event.getStatus()).as(id).isEqualTo("ok");
            assertThat(event.getErrorMessage()).as(id).isNull();
            assertThat(event.getValue()).as(id).isEqualTo(value);
            assertThat(event.getUnit()).as(id).isEqualTo("count");
            assertThat(event.getGroups()).as(id).containsExactly("business", "affiliation");
        });
    }

    @Test
    void nominal_usesTheCommissionWindowsAndParams() {
        FakeClickStats clicks = new FakeClickStats(STATS, null);
        Map<String, MetriksEvent> events = clickEvents(clicks);

        assertThat(events.get("affiliation.awin.clicks.7d").getParams()).containsExactly(
                Map.entry("provider", "open4goods-api"), Map.entry("network", "awin"),
                Map.entry("windowStartDate", "2026-10-15"), Map.entry("windowEndDate", "2026-10-21"));
        assertThat(events.get("affiliation.total.clicks.28d").getParams())
                .containsEntry("network", "total")
                .containsEntry("windowStartDate", "2026-09-24")
                .containsEntry("windowEndDate", "2026-10-21")
                .doesNotContainKey("currency");
        assertThat(events.get("affiliation.other.clicks.28d").getTags()).containsExactly("affiliation", "other");

        // One aggregation request, bounded by Paris midnights: [D-28, D) and [D-7, D), top 20.
        assertThat(clicks.calls).hasSize(1);
        long[] call = clicks.calls.get(0);
        assertThat(Instant.ofEpochMilli(call[0])).isEqualTo(Instant.parse("2026-09-23T22:00:00Z"));
        assertThat(Instant.ofEpochMilli(call[1])).isEqualTo(Instant.parse("2026-10-14T22:00:00Z"));
        assertThat(Instant.ofEpochMilli(call[2])).isEqualTo(Instant.parse("2026-10-21T22:00:00Z"));
        assertThat(call[3]).isEqualTo(20);
    }

    @Test
    void nominal_reportsTheTopMerchantsAsRows() {
        MetriksEvent top = clickEvents(new FakeClickStats(STATS, null)).get("affiliation.clicks.top_merchants.28d");

        assertThat(top.getStatus()).isEqualTo("ok");
        assertThat(top.getValue()).isEqualTo(2);
        assertThat(top.getParams()).containsEntry("windowStartDate", "2026-09-24").containsEntry("windowEndDate", "2026-10-21");
        assertThat(top.getParams().get("rows")).isEqualTo(List.of(
                Map.of("merchant", "darty.com", "network", "awin", "clicks", 7L),
                Map.of("merchant", "boulanger.com", "network", "effiliation", "clicks", 4L)));
    }

    @Test
    void noClick_isZeroNotError() {
        Map<String, MetriksEvent> events = clickEvents(AffiliationTestSupport.noClicks());

        events.values().forEach(event -> assertThat(event.getStatus()).as(event.getId()).isEqualTo("ok"));
        assertThat(events.get("affiliation.total.clicks.28d").getValue()).isEqualTo(0L);
        assertThat(events.get("affiliation.clicks.top_merchants.28d").getValue()).isEqualTo(0);
        assertThat(events.get("affiliation.clicks.top_merchants.28d").getParams().get("rows")).isEqualTo(List.of());
    }

    @Test
    void elasticsearchDown_putsEveryClickEventInErrorNeverZero() {
        Map<String, MetriksEvent> events = clickEvents(new FakeClickStats(null,
                new DataAccessResourceFailureException("Connection refused: http://elastic:secret@10.0.0.1:9200")));

        assertThat(events.keySet()).containsExactlyElementsOf(CLICK_IDS);
        List<String> messages = new ArrayList<>();
        events.values().forEach(event -> {
            assertThat(event.getStatus()).as(event.getId()).isEqualTo("error");
            assertThat(event.getValue()).as(event.getId()).isNull();
            messages.add(event.getErrorMessage());
        });
        assertThat(messages).containsOnly(
                "Clics indisponibles : Elasticsearch en erreur (DataAccessResourceFailureException)");
        assertThat(events.get("affiliation.clicks.top_merchants.28d").getParams()).doesNotContainKey("rows");
    }

    @Test
    void missingIndex_putsEveryClickEventInError() {
        Map<String, MetriksEvent> events = clickEvents(new FakeClickStats(null,
                new NoSuchIndexException("affiliation-clicks")));

        events.values().forEach(event -> {
            assertThat(event.getStatus()).as(event.getId()).isEqualTo("error");
            assertThat(event.getValue()).as(event.getId()).isNull();
            assertThat(event.getErrorMessage()).as(event.getId())
                    .isEqualTo("Clics indisponibles : index affiliation-clicks absent");
        });
    }

    @Test
    void clickFailure_doesNotTouchTheCommissionEvents() {
        List<MetriksEvent> all = collectAll(new FakeClickStats(null, new IllegalStateException("boom")));

        all.stream().filter(event -> !event.getId().contains(".clicks"))
                .forEach(event -> assertThat(event.getStatus()).as(event.getId()).isEqualTo("ok"));
    }
}
