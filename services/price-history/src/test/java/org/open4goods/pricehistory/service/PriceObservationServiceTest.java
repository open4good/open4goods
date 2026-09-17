package org.open4goods.pricehistory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.PayloadHash;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.pricehistory.model.OfferAvailability;
import org.open4goods.pricehistory.model.CompletedOfferFeed;
import org.open4goods.pricehistory.model.OfferCondition;
import org.open4goods.pricehistory.model.OfferHead;
import org.open4goods.pricehistory.model.OfferKey;
import org.open4goods.pricehistory.model.OfferObservation;
import org.open4goods.pricehistory.model.PriceChangeEvent;
import org.open4goods.pricehistory.model.PriceChangeKind;
import org.open4goods.pricehistory.port.OfferHeadStore;
import org.open4goods.pricehistory.port.PriceChangeEventStore;

/** Verifies sparse events and recoverable pending-event delivery. */
class PriceObservationServiceTest {

    @Test
    void unchangedPresenceRefreshesOnlyTheHeadWhileChangesAppendOneEvent() {
        InMemoryHeads heads = new InMemoryHeads();
        InMemoryEvents events = new InMemoryEvents();
        PriceObservationService service = new PriceObservationService(heads, events);
        Instant start = Instant.parse("2026-09-15T00:00:00Z");

        assertThat(service.ingest(observation(start, "aa", "10.00"))).isEqualTo(PriceChangeKind.FIRST_SEEN);
        assertThat(service.ingest(observation(start.plusSeconds(30), "aa", "10.00"))).isNull();
        assertThat(service.ingest(observation(start.plusSeconds(60), "bb", "12.00"))).isEqualTo(PriceChangeKind.CHANGED);

        assertThat(events.events()).extracting(PriceChangeEvent::kind)
                .containsExactly(PriceChangeKind.FIRST_SEEN, PriceChangeKind.CHANGED);
        assertThat(heads.find(key()).orElseThrow().lastSeenAt()).isEqualTo(start.plusSeconds(60));
        assertThat(heads.find(key()).orElseThrow().pendingEvent()).isNull();
    }

    @Test
    void aRetryDeliversThePendingEventBeforeAdmittingANewerObservation() {
        InMemoryHeads heads = new InMemoryHeads();
        InMemoryEvents events = new InMemoryEvents();
        PriceObservationService service = new PriceObservationService(heads, events);
        Instant start = Instant.parse("2026-09-15T00:00:00Z");
        OfferObservation first = observation(start, "aa", "10.00");
        PriceChangeEvent pending = PriceChangeEvent.of(first, PriceChangeKind.FIRST_SEEN);
        heads.compareAndSet(OfferHead.first(first, pending), 0);

        assertThat(service.ingest(observation(start.plusSeconds(1), "bb", "11.00"))).isEqualTo(PriceChangeKind.CHANGED);
        assertThat(events.events()).extracting(PriceChangeEvent::kind)
                .containsExactly(PriceChangeKind.FIRST_SEEN, PriceChangeKind.CHANGED);
    }

    @Test
    void distinctChangesAtOneObservationInstantReceiveDistinctTsdsTimestamps() {
        InMemoryHeads heads = new InMemoryHeads();
        InMemoryEvents events = new InMemoryEvents();
        PriceObservationService service = new PriceObservationService(heads, events);
        Instant observed = Instant.parse("2026-09-15T00:00:00Z");

        service.ingest(observation(observed, "aa", "10.00"));
        assertThat(service.ingest(observation(observed, "bb", "11.00"))).isEqualTo(PriceChangeKind.CHANGED);

        assertThat(events.events()).hasSize(2);
        assertThat(events.events().getFirst().persistenceTimestamp())
                .isNotEqualTo(events.events().getLast().persistenceTimestamp());
    }

    @Test
    void aSuccessfulCompleteFeedAloneCanMarkAnOfferAsDisappeared() {
        InMemoryHeads heads = new InMemoryHeads();
        InMemoryEvents events = new InMemoryEvents();
        PriceObservationService service = new PriceObservationService(heads, events);
        Instant start = Instant.parse("2026-09-15T00:00:00Z");
        service.ingest(observation(start, "aa", "10.00"));

        assertThat(service.reconcileCompletedFeed(new CompletedOfferFeed(new SourceId("fixture"), start.plusSeconds(60),
                Set.of()))).isEqualTo(1);

        assertThat(events.events()).extracting(PriceChangeEvent::kind)
                .containsExactly(PriceChangeKind.FIRST_SEEN, PriceChangeKind.DISAPPEARED);
        assertThat(heads.find(key()).orElseThrow().observation().availability()).isEqualTo(OfferAvailability.UNAVAILABLE);
    }

    @Test
    void anOlderObservationCannotRollBackTheCurrentOffer() {
        InMemoryHeads heads = new InMemoryHeads();
        InMemoryEvents events = new InMemoryEvents();
        PriceObservationService service = new PriceObservationService(heads, events);
        Instant start = Instant.parse("2026-09-15T00:00:00Z");
        service.ingest(observation(start.plusSeconds(60), "cc", "12.00"));

        assertThat(service.ingest(observation(start, "dd", "10.00"))).isNull();
        assertThat(heads.find(key()).orElseThrow().amount()).isEqualByComparingTo("12.00");
        assertThat(events.events()).hasSize(1);
    }

    @Test
    void aFailedEventWriteRemainsPendingUntilTheRetryReconcilesIt() {
        InMemoryHeads heads = new InMemoryHeads();
        FailOnceEvents events = new FailOnceEvents();
        PriceObservationService service = new PriceObservationService(heads, events);
        Instant start = Instant.parse("2026-09-15T00:00:00Z");

        assertThatThrownBy(() -> service.ingest(observation(start, "aa", "10.00")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(heads.find(key()).orElseThrow().pendingEvent()).isNotNull();
        assertThat(service.ingest(observation(start, "aa", "10.00"))).isNull();
        assertThat(events.events()).extracting(PriceChangeEvent::kind).containsExactly(PriceChangeKind.FIRST_SEEN);
        assertThat(heads.find(key()).orElseThrow().pendingEvent()).isNull();
    }

    private static OfferObservation observation(Instant observedAt, String hash, String amount) {
        return new OfferObservation(key(), OfferCondition.NEW, Currency.getInstance("EUR"), new BigDecimal(amount),
                OfferAvailability.AVAILABLE, observedAt, observedAt, new PayloadHash("SHA-256", hash),
                new SourceUsagePolicyRef("fixture", "1"));
    }

    private static OfferKey key() {
        return new OfferKey(new Gtin("0123456789012"), new SourceId("fixture"), "offer-1");
    }

    private static final class InMemoryHeads implements OfferHeadStore {
        private final Map<OfferKey, OfferHead> values = new LinkedHashMap<>();

        @Override
        public Optional<OfferHead> find(OfferKey key) {
            return Optional.ofNullable(values.get(key));
        }

        @Override
        public boolean compareAndSet(OfferHead head, long expectedRevision) {
            OfferHead current = values.get(head.observation().key());
            if ((current == null && expectedRevision != 0)
                    || (current != null && current.revision() != expectedRevision)) {
                return false;
            }
            values.put(head.observation().key(), head);
            return true;
        }

        @Override
        public Stream<OfferHead> findByProvider(SourceId providerId) {
            return values.values().stream().filter(head -> head.observation().key().providerId().equals(providerId));
        }
    }

    private static class InMemoryEvents implements PriceChangeEventStore {
        private final Map<String, PriceChangeEvent> values = new LinkedHashMap<>();

        @Override
        public boolean append(PriceChangeEvent event) {
            return values.putIfAbsent(event.id(), event) == null;
        }

        List<PriceChangeEvent> events() {
            return new ArrayList<>(values.values());
        }
    }

    private static final class FailOnceEvents extends InMemoryEvents {
        private boolean fail = true;

        @Override
        public boolean append(PriceChangeEvent event) {
            if (fail) {
                fail = false;
                throw new IllegalStateException("fixture write failure");
            }
            return super.append(event);
        }
    }
}
