package org.open4goods.pricehistory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.datareference.serialization.DataReferenceJson;
import org.open4goods.pricehistory.model.LegacyMinimumPricePoint;
import org.open4goods.pricehistory.model.LegacyPriceBackfill;
import org.open4goods.pricehistory.model.OfferCondition;
import org.open4goods.pricehistory.model.PriceHistoryGranularity;
import org.open4goods.pricehistory.model.PriceHistoryQuery;
import org.open4goods.pricehistory.port.LegacyPriceBackfillStore;

/** Verifies query defaults, opaque pagination, and neutral legacy import idempotence. */
class PriceHistoryContractTest {

    @Test
    void longRangesDefaultToDailyAndTheCursorRoundTrips() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        PriceHistoryQuery query = new PriceHistoryQuery(from, from.plusSeconds(32L * 86_400), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 50, false);
        String cursor = PriceHistoryCursor.after(from, "event-1");

        assertThat(query.effectiveGranularity()).isEqualTo(PriceHistoryGranularity.DAY);
        assertThat(PriceHistoryCursor.decode(cursor).timestamp()).isEqualTo(from);
        assertThat(PriceHistoryCursor.decode(cursor).stableId()).isEqualTo("event-1");
    }

    /** GOU-100: the public `limit` bound is 1..500, tracking {@link PriceHistoryQuery#MAX_PAGE_SIZE}. */
    @Test
    void pageSizeAcceptsTheLowerAndUpperBound() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = from.plusSeconds(86_400);

        PriceHistoryQuery lowerBound = new PriceHistoryQuery(from, to, Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                PriceHistoryQuery.MIN_PAGE_SIZE, false);
        PriceHistoryQuery upperBound = new PriceHistoryQuery(from, to, Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                PriceHistoryQuery.MAX_PAGE_SIZE, false);

        assertThat(lowerBound.pageSize()).isEqualTo(1);
        assertThat(upperBound.pageSize()).isEqualTo(500);
        assertThat(PriceHistoryQuery.MAX_PAGE_SIZE).isEqualTo(500);
    }

    @Test
    void pageSizeRejectsBothOutOfRangeSides() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = from.plusSeconds(86_400);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PriceHistoryQuery(from, to, Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        PriceHistoryQuery.MIN_PAGE_SIZE - 1, false))
                .withMessageContaining("pageSize must be between");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PriceHistoryQuery(from, to, Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        PriceHistoryQuery.MAX_PAGE_SIZE + 1, false))
                .withMessageContaining("pageSize must be between");
    }

    /** A cursor from one page is only meaningful when replayed with the same bounded page size. */
    @Test
    void cursorRoundTripsAtTheBoundedPageSize() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = from.plusSeconds(86_400);
        String cursor = PriceHistoryCursor.after(from, "event-1");

        PriceHistoryQuery nextPage = new PriceHistoryQuery(from, to, Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(cursor),
                PriceHistoryQuery.MAX_PAGE_SIZE, false);

        assertThat(nextPage.cursor()).contains(cursor);
        assertThat(nextPage.pageSize()).isEqualTo(PriceHistoryQuery.MAX_PAGE_SIZE);
        assertThat(PriceHistoryCursor.decode(nextPage.cursor().orElseThrow()).stableId()).isEqualTo("event-1");
    }

    @Test
    void legacyImportUsesASeparateRepeatSafeShape() {
        CountingStore store = new CountingStore();
        LegacyPriceBackfillService service = new LegacyPriceBackfillService(store);
        LegacyMinimumPricePoint point = new LegacyMinimumPricePoint(new Gtin("0123456789012"), OfferCondition.NEW,
                Currency.getInstance("EUR"), new BigDecimal("10.00"), Instant.parse("2020-01-01T00:00:00Z"),
                new SourceUsagePolicyRef("legacy-product", "1"), "product/0123456789012/new/2020-01-01");

        assertThat(service.importOnce(point)).isTrue();
        assertThat(service.importOnce(point)).isFalse();
        assertThat(store.value.id()).startsWith("legacy-price:");
    }

    /** Ensures same-observation transitions retain the TSDS identity precision they require. */
    @Test
    void timeSeriesTemplatesRetainNanosecondTimestampPrecision() throws IOException {
        assertThat(timestampType("elasticsearch/price-change-index-template.json")).isEqualTo("date_nanos");
        assertThat(timestampType("elasticsearch/daily-provider-rollup-index-template.json")).isEqualTo("date_nanos");
    }

    /** Reads the timestamp mapping from a versioned Elasticsearch template resource. */
    private static String timestampType(String resource) throws IOException {
        try (var stream = PriceHistoryContractTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(stream).as("resource %s", resource).isNotNull();
            JsonNode template = DataReferenceJson.mapper().readTree(stream);
            return template.path("template").path("mappings").path("properties").path("@timestamp").path("type").asString();
        }
    }

    private static final class CountingStore implements LegacyPriceBackfillStore {
        private LegacyPriceBackfill value;

        @Override
        public boolean append(LegacyPriceBackfill backfill) {
            if (value != null && value.id().equals(backfill.id())) {
                return false;
            }
            value = backfill;
            return true;
        }
    }
}
