package org.open4goods.pricehistory.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

/**
 * One observed offer presence used by daily aggregation independently of sparse changes.
 *
 * @param key offer identity
 * @param condition advertised condition
 * @param currency advertised currency
 * @param amount advertised amount
 * @param observedAt successful O4G observation time
 */
public record DailyOfferPresence(
        OfferKey key, OfferCondition condition, Currency currency, BigDecimal amount, Instant observedAt) {

    /** Validates a daily presence contribution. */
    public DailyOfferPresence {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(condition, "condition must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        amount = amount.stripTrailingZeros();
        Objects.requireNonNull(observedAt, "observedAt must not be null");
    }

    /** Returns daily dimensions for this offer presence. */
    public DailyRollupKey rollupKey() {
        return new DailyRollupKey(key.gtin(), key.providerId(), condition, currency);
    }

    /** Uses an immutable current head as one independently materialized presence input. */
    public static DailyOfferPresence from(OfferHead head) {
        Objects.requireNonNull(head, "head must not be null");
        OfferObservation observation = head.observation();
        return new DailyOfferPresence(observation.key(), observation.condition(), observation.currency(),
                observation.amount(), observation.observedAt());
    }
}
