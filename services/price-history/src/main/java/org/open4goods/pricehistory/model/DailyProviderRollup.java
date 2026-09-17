package org.open4goods.pricehistory.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Query-serving provider rollup for one UTC date and dimensions.
 *
 * @param key provider/product dimensions
 * @param day UTC calendar day
 * @param minimumAmount lowest present-offer amount
 * @param maximumAmount highest present-offer amount
 * @param closeAmount last amount under observation-time then stable-key ordering
 * @param observedOfferCount distinct offers observed present
 * @param firstObservedAt first presence in the bucket
 * @param lastObservedAt last presence in the bucket
 * @param changeCount sparse event transitions in the bucket
 */
public record DailyProviderRollup(
        DailyRollupKey key,
        LocalDate day,
        BigDecimal minimumAmount,
        BigDecimal maximumAmount,
        BigDecimal closeAmount,
        long observedOfferCount,
        Instant firstObservedAt,
        Instant lastObservedAt,
        long changeCount) {

    /** Validates a fully materialized rollup. */
    public DailyProviderRollup {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(day, "day must not be null");
        Objects.requireNonNull(minimumAmount, "minimumAmount must not be null");
        Objects.requireNonNull(maximumAmount, "maximumAmount must not be null");
        Objects.requireNonNull(closeAmount, "closeAmount must not be null");
        if (minimumAmount.signum() < 0 || maximumAmount.signum() < 0 || closeAmount.signum() < 0) {
            throw new IllegalArgumentException("rollup amounts must not be negative");
        }
        if (minimumAmount.compareTo(maximumAmount) > 0
                || closeAmount.compareTo(minimumAmount) < 0 || closeAmount.compareTo(maximumAmount) > 0) {
            throw new IllegalArgumentException("rollup amount bounds are inconsistent");
        }
        if (observedOfferCount < 1 || changeCount < 0) {
            throw new IllegalArgumentException("rollup counts are invalid");
        }
        Objects.requireNonNull(firstObservedAt, "firstObservedAt must not be null");
        Objects.requireNonNull(lastObservedAt, "lastObservedAt must not be null");
        if (firstObservedAt.isAfter(lastObservedAt)) {
            throw new IllegalArgumentException("firstObservedAt must not be after lastObservedAt");
        }
    }
}
