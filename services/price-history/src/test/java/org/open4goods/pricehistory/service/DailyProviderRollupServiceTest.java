package org.open4goods.pricehistory.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.PayloadHash;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.pricehistory.model.DailyOfferPresence;
import org.open4goods.pricehistory.model.DailyProviderRollup;
import org.open4goods.pricehistory.model.DailyRollupKey;
import org.open4goods.pricehistory.model.DailyRollupWindow;
import org.open4goods.pricehistory.model.OfferAvailability;
import org.open4goods.pricehistory.model.OfferCondition;
import org.open4goods.pricehistory.model.OfferKey;
import org.open4goods.pricehistory.model.OfferObservation;
import org.open4goods.pricehistory.model.PriceChangeEvent;
import org.open4goods.pricehistory.model.PriceChangeKind;
import org.open4goods.pricehistory.port.DailyProviderRollupStore;

/** Verifies daily presence rollups independently of sparse price-change events. */
class DailyProviderRollupServiceTest {

    @Test
    void unchangedOffersContributePresenceAndCloseUsesStableTieBreak() {
        Instant now = Instant.parse("2026-09-15T12:00:00Z");
        InMemoryStore store = new InMemoryStore();
        DailyProviderRollupService service = new DailyProviderRollupService(store,
                new DailyRollupWindow(Clock.fixed(now, ZoneOffset.UTC)));
        Instant observed = Instant.parse("2026-09-15T10:00:00Z");
        DailyOfferPresence first = presence("offer-a", "10.00", observed);
        DailyOfferPresence second = presence("offer-b", "12.00", observed);
        PriceChangeEvent event = PriceChangeEvent.of(observation("offer-a", "10.00", observed), PriceChangeKind.FIRST_SEEN);

        assertThat(service.updateOpenBuckets(Stream.of(first, second), Stream.of(event))).isEqualTo(1);

        DailyProviderRollup rollup = store.find(first.rollupKey(), LocalDate.of(2026, 9, 15)).orElseThrow();
        assertThat(rollup.minimumAmount()).isEqualByComparingTo("10");
        assertThat(rollup.maximumAmount()).isEqualByComparingTo("12");
        assertThat(rollup.closeAmount()).isEqualByComparingTo("12");
        assertThat(rollup.observedOfferCount()).isEqualTo(2);
        assertThat(rollup.changeCount()).isEqualTo(1);
    }

    @Test
    void olderLateBucketsRequireExplicitRebuild() {
        Instant now = Instant.parse("2026-09-15T12:00:00Z");
        InMemoryStore store = new InMemoryStore();
        DailyProviderRollupService service = new DailyProviderRollupService(store,
                new DailyRollupWindow(Clock.fixed(now, ZoneOffset.UTC)));
        DailyOfferPresence old = presence("offer-a", "10.00", Instant.parse("2026-09-12T10:00:00Z"));

        assertThat(service.updateOpenBuckets(Stream.of(old), Stream.empty())).isZero();
        assertThat(service.rebuild(Stream.of(old), Stream.empty())).isEqualTo(1);
    }

    private static DailyOfferPresence presence(String offerId, String amount, Instant observedAt) {
        OfferObservation observation = observation(offerId, amount, observedAt);
        return new DailyOfferPresence(observation.key(), observation.condition(), observation.currency(), observation.amount(),
                observation.observedAt());
    }

    private static OfferObservation observation(String offerId, String amount, Instant observedAt) {
        return new OfferObservation(new OfferKey(new Gtin("0123456789012"), new SourceId("fixture"), offerId),
                OfferCondition.NEW, Currency.getInstance("EUR"), new BigDecimal(amount), OfferAvailability.AVAILABLE,
                observedAt, observedAt, new PayloadHash("SHA-256", offerId.endsWith("a") ? "aa" : "bb"),
                new SourceUsagePolicyRef("fixture", "1"));
    }

    private static final class InMemoryStore implements DailyProviderRollupStore {
        private final Map<String, DailyProviderRollup> values = new LinkedHashMap<>();

        @Override
        public Optional<DailyProviderRollup> find(DailyRollupKey key, LocalDate day) {
            return Optional.ofNullable(values.get(key.toString() + day));
        }

        @Override
        public void upsert(DailyProviderRollup rollup) {
            values.put(rollup.key().toString() + rollup.day(), rollup);
        }
    }
}
