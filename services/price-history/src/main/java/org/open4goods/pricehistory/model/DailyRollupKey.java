package org.open4goods.pricehistory.model;

import java.util.Currency;
import java.util.Objects;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.SourceId;

/** Provider, product, condition, and currency dimensions of one daily rollup. */
public record DailyRollupKey(
        Gtin gtin, SourceId providerId, OfferCondition condition, Currency currency) {

    /** Validates the provider-neutral dimensions. */
    public DailyRollupKey {
        Objects.requireNonNull(gtin, "gtin must not be null");
        Objects.requireNonNull(providerId, "providerId must not be null");
        Objects.requireNonNull(condition, "condition must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
    }

    /** Derives the rollup dimensions from one offer identity and observed state. */
    public static DailyRollupKey of(OfferObservation observation) {
        Objects.requireNonNull(observation, "observation must not be null");
        return new DailyRollupKey(observation.key().gtin(), observation.key().providerId(), observation.condition(),
                observation.currency());
    }
}
