package org.open4goods.datareference.model.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Current offer state supplied to a reference projection.
 *
 * <p>This is a provider-neutral summary, not an offer assertion and not price
 * history. The price-observation WorkOrder owns the producer.
 *
 * @param activeOfferCount number of current offers
 * @param available whether at least one current offer can be bought
 * @param lowestPrice lowest current price, or {@code null} when unavailable
 * @param currency ISO 4217 currency for {@code lowestPrice}, or {@code null}
 * @param observedAt fixed instant at which the summary was read
 */
public record OfferSummary(int activeOfferCount, boolean available, BigDecimal lowestPrice,
        String currency, Instant observedAt) {

    /** Validates an internally coherent current-offer summary. */
    public OfferSummary {
        if (activeOfferCount < 0) {
            throw new IllegalArgumentException("active offer count must not be negative");
        }
        Objects.requireNonNull(observedAt, "observedAt must not be null");
        if (available != (activeOfferCount > 0)) {
            throw new IllegalArgumentException("availability must match the active offer count");
        }
        if (lowestPrice != null && lowestPrice.signum() < 0) {
            throw new IllegalArgumentException("lowest price must not be negative");
        }
        if ((lowestPrice == null) != (currency == null)) {
            throw new IllegalArgumentException("price and currency must either both be present or both be absent");
        }
        if (currency != null && !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("currency must be an uppercase ISO 4217 code");
        }
    }
}
