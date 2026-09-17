package org.open4goods.pricehistory.model;

import java.util.Objects;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.SourceId;

/**
 * Stable identity of one provider offer.
 *
 * @param gtin normalized product identity
 * @param providerId public provider identity
 * @param providerOfferId provider-local offer identity
 */
public record OfferKey(Gtin gtin, SourceId providerId, String providerOfferId) {

    /** Validates the provider-neutral identity coordinates. */
    public OfferKey {
        Objects.requireNonNull(gtin, "gtin must not be null");
        Objects.requireNonNull(providerId, "providerId must not be null");
        if (providerOfferId == null || !providerOfferId.matches("[A-Za-z0-9._:-]{1,240}")) {
            throw new IllegalArgumentException("providerOfferId must be a bounded opaque identifier");
        }
    }

    /**
     * Returns a stable human-readable identity that contains no price evidence.
     *
     * @return provider-neutral offer key
     */
    public String externalForm() {
        return gtin.value() + ":" + providerId.value() + ":" + providerOfferId;
    }
}
