package org.open4goods.pricehistory.service;

import static org.assertj.core.api.Assertions.assertThat;

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
