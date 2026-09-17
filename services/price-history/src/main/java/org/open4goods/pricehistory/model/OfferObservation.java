package org.open4goods.pricehistory.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

import org.open4goods.datareference.model.PayloadHash;
import org.open4goods.datareference.model.SourceUsagePolicyRef;

/**
 * One provider observation, before it is compared with the current offer head.
 *
 * @param key offer identity
 * @param condition advertised condition
 * @param currency ISO 4217 currency
 * @param amount advertised amount
 * @param availability advertised availability
 * @param providerObservedAt instant represented by the provider feed
 * @param observedAt instant O4G observed the feed
 * @param contentHash stable digest of the provider observation
 * @param policyRef usage policy applied at observation time
 */
public record OfferObservation(
        OfferKey key,
        OfferCondition condition,
        Currency currency,
        BigDecimal amount,
        OfferAvailability availability,
        Instant providerObservedAt,
        Instant observedAt,
        PayloadHash contentHash,
        SourceUsagePolicyRef policyRef) {

    /** Validates one complete provider offer observation. */
    public OfferObservation {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(condition, "condition must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        amount = amount.stripTrailingZeros();
        Objects.requireNonNull(availability, "availability must not be null");
        Objects.requireNonNull(providerObservedAt, "providerObservedAt must not be null");
        Objects.requireNonNull(observedAt, "observedAt must not be null");
        Objects.requireNonNull(contentHash, "contentHash must not be null");
        Objects.requireNonNull(policyRef, "policyRef must not be null");
    }
}
