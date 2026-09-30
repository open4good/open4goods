package org.open4goods.pricehistory.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.SourceUsagePolicyRef;

/**
 * Original legacy Product minimum-price point, deliberately without inferred provider or presence.
 *
 * @param gtin normalized product identity
 * @param condition legacy condition
 * @param currency original ISO 4217 currency
 * @param amount original price amount
 * @param recordedAt original legacy timestamp
 * @param provenance explicit source of the old Product value
 * @param sourceCoordinate stable legacy input coordinate for idempotence
 */
public record LegacyMinimumPricePoint(
        Gtin gtin,
        OfferCondition condition,
        Currency currency,
        BigDecimal amount,
        Instant recordedAt,
        SourceUsagePolicyRef provenance,
        String sourceCoordinate) {

    /** Validates a neutral legacy point. */
    public LegacyMinimumPricePoint {
        Objects.requireNonNull(gtin, "gtin must not be null");
        Objects.requireNonNull(condition, "condition must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        amount = amount.stripTrailingZeros();
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        Objects.requireNonNull(provenance, "provenance must not be null");
        if (sourceCoordinate == null || sourceCoordinate.isBlank() || sourceCoordinate.length() > 512) {
            throw new IllegalArgumentException("sourceCoordinate must be bounded and nonblank");
        }
    }
}
