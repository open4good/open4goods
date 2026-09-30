package org.open4goods.pricehistory.service;

import java.util.Objects;
import java.util.Set;

import org.open4goods.pricehistory.model.OfferHead;
import org.open4goods.pricehistory.model.OfferAvailability;
import org.open4goods.pricehistory.model.CompletedOfferFeed;
import org.open4goods.pricehistory.model.OfferKey;
import org.open4goods.pricehistory.model.OfferObservation;
import org.open4goods.pricehistory.model.PriceChangeEvent;
import org.open4goods.pricehistory.model.PriceChangeKind;
import org.open4goods.pricehistory.port.OfferHeadStore;
import org.open4goods.pricehistory.port.PriceChangeEventStore;

/**
 * Compares provider observations with their current offer head.
 *
 * <p>The head first receives a pending event through CAS. The event is then appended by its
 * deterministic identity and the marker is cleared with another CAS. A retry always finishes a
 * pending event before accepting a following observation, so a failure between two Elasticsearch
 * documents cannot drop a price event.
 */
public final class PriceObservationService {

    private final OfferHeadStore heads;
    private final PriceChangeEventStore events;

    /** Creates the provider-neutral ingestion service. */
    public PriceObservationService(OfferHeadStore heads, PriceChangeEventStore events) {
        this.heads = Objects.requireNonNull(heads, "heads must not be null");
        this.events = Objects.requireNonNull(events, "events must not be null");
    }

    /**
     * Records one observation and returns its semantic transition kind.
     *
     * @param observation provider observation
     * @return transition kind, or {@code null} when only presence changed
     */
    public PriceChangeKind ingest(OfferObservation observation) {
        Objects.requireNonNull(observation, "observation must not be null");
        for (int retry = 0; retry < 16; retry++) {
            OfferHead current = heads.find(observation.key()).orElse(null);
            if (current != null && current.pendingEvent() != null) {
                deliverPending(current);
                continue;
            }
            if (current != null && observation.observedAt().isBefore(current.observation().observedAt())) {
                return null;
            }
            PriceChangeKind kind = transitionKind(current, observation);
            PriceChangeEvent event = kind == null ? null : PriceChangeEvent.of(observation, kind);
            if (current != null && observation.observedAt().equals(current.observation().observedAt())) {
                if (event != null && !current.contentHash().equals(observation.contentHash())) {
                    // A TSDS dimension/timestamp identity rejects a second document at the exact
                    // same instant. PriceChangeEvent derives a stable nanosecond offset from its
                    // content digest, so this historical transition remains queryable even though
                    // a deterministic tie-break keeps the current head unchanged.
                    events.append(event);
                    return kind;
                }
                return null;
            }
            OfferHead replacement = current == null ? OfferHead.first(observation, event) : current.replace(observation, event);
            long expected = current == null ? 0 : current.revision();
            if (!heads.compareAndSet(replacement, expected)) {
                continue;
            }
            if (event != null) {
                deliverPending(replacement);
            }
            return kind;
        }
        throw new IllegalStateException("offer head remained concurrently modified after 16 retries");
    }

    /**
     * Marks absent offers unavailable only after a provider's complete feed succeeded.
     *
     * <p>An incomplete or failed feed must never be represented as disappearance. The retained
     * head supplies the price, condition, policy, and last provider timestamp; the completed
     * feed supplies the O4G observation instant.
     *
     * @param feed successful complete-feed boundary
     * @return number of heads transitioned to unavailable
     */
    public int reconcileCompletedFeed(CompletedOfferFeed feed) {
        Objects.requireNonNull(feed, "feed must not be null");
        Set<OfferKey> published = feed.publishedOfferKeys();
        return (int) heads.findByProvider(feed.providerId())
                .filter(head -> head.observation().availability() != OfferAvailability.UNAVAILABLE)
                .filter(head -> !published.contains(head.observation().key()))
                .map(head -> new OfferObservation(head.observation().key(), head.observation().condition(),
                        head.observation().currency(), head.observation().amount(), OfferAvailability.UNAVAILABLE,
                        head.observation().providerObservedAt(), feed.completedAt(), head.contentHash(), head.policyRef()))
                .map(this::ingest)
                .filter(PriceChangeKind.DISAPPEARED::equals)
                .count();
    }

    private PriceChangeKind transitionKind(OfferHead current, OfferObservation observation) {
        if (current == null) {
            return PriceChangeKind.FIRST_SEEN;
        }
        if (current.observation().availability() == OfferAvailability.UNAVAILABLE
                && observation.availability() == OfferAvailability.AVAILABLE) {
            return PriceChangeKind.REAPPEARED;
        }
        if (current.observation().availability() != OfferAvailability.UNAVAILABLE
                && observation.availability() == OfferAvailability.UNAVAILABLE) {
            return PriceChangeKind.DISAPPEARED;
        }
        return current.changesFrom(observation) ? PriceChangeKind.CHANGED : null;
    }

    private void deliverPending(OfferHead pending) {
        PriceChangeEvent event = pending.pendingEvent();
        events.append(event);
        OfferHead cleared = new OfferHead(pending.observation(), pending.firstSeenAt(), pending.lastSeenAt(),
                pending.revision(), null);
        heads.compareAndSet(cleared, pending.revision());
    }
}
