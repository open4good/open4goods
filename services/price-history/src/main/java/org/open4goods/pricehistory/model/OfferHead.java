package org.open4goods.pricehistory.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

import org.open4goods.datareference.model.PayloadHash;
import org.open4goods.datareference.model.SourceUsagePolicyRef;

/**
 * Replaceable latest state of a provider offer.
 *
 * @param observation current provider observation
 * @param firstSeenAt first O4G observation of this offer
 * @param lastSeenAt latest successful observation of this offer
 * @param revision monotonically increasing CAS revision
 * @param pendingEvent deterministic event awaiting durable delivery, or {@code null}
 */
public record OfferHead(
        OfferObservation observation,
        Instant firstSeenAt,
        Instant lastSeenAt,
        long revision,
        PriceChangeEvent pendingEvent) {

    /** Validates chronology and pending-event ownership. */
    public OfferHead {
        Objects.requireNonNull(observation, "observation must not be null");
        Objects.requireNonNull(firstSeenAt, "firstSeenAt must not be null");
        Objects.requireNonNull(lastSeenAt, "lastSeenAt must not be null");
        if (firstSeenAt.isAfter(lastSeenAt)) {
            throw new IllegalArgumentException("firstSeenAt must not be after lastSeenAt");
        }
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be positive");
        }
        if (pendingEvent != null && !pendingEvent.key().equals(observation.key())) {
            throw new IllegalArgumentException("pending event must belong to the offer head");
        }
    }

    /**
     * Builds the first durable offer head.
     *
     * @param observation provider observation
     * @param pendingEvent first-sighting event awaiting delivery
     * @return initial head
     */
    public static OfferHead first(OfferObservation observation, PriceChangeEvent pendingEvent) {
        return new OfferHead(observation, observation.observedAt(), observation.observedAt(), 1, pendingEvent);
    }

    /**
     * Returns whether an observation changes price-event semantics, not mere presence.
     *
     * @param candidate provider observation
     * @return whether an immutable event is needed
     */
    public boolean changesFrom(OfferObservation candidate) {
        Objects.requireNonNull(candidate, "candidate must not be null");
        return !observation.amount().equals(candidate.amount())
                || !observation.currency().equals(candidate.currency())
                || observation.condition() != candidate.condition()
                || observation.availability() != candidate.availability();
    }

    /**
     * Copies a newer observation with its next CAS revision and optional pending event.
     *
     * @param candidate accepted newer observation
     * @param event event to deliver, or {@code null} for unchanged presence
     * @return replacement head
     */
    public OfferHead replace(OfferObservation candidate, PriceChangeEvent event) {
        Objects.requireNonNull(candidate, "candidate must not be null");
        if (!observation.key().equals(candidate.key())) {
            throw new IllegalArgumentException("replacement must retain offer identity");
        }
        if (candidate.observedAt().isBefore(observation.observedAt())) {
            throw new IllegalArgumentException("replacement must not roll back the current observation");
        }
        return new OfferHead(candidate, firstSeenAt, candidate.observedAt(), revision + 1, event);
    }

    /** Returns the current amount without exposing a mutable numeric representation. */
    public BigDecimal amount() {
        return observation.amount();
    }

    /** Returns the current ISO 4217 currency. */
    public Currency currency() {
        return observation.currency();
    }

    /** Returns the policy recorded with the current observation. */
    public SourceUsagePolicyRef policyRef() {
        return observation.policyRef();
    }

    /** Returns the current observation digest. */
    public PayloadHash contentHash() {
        return observation.contentHash();
    }
}
