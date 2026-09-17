package org.open4goods.pricehistory.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

import org.open4goods.datareference.model.SourceId;

/**
 * Successful boundary of a provider feed used to detect true offer disappearances.
 *
 * @param providerId public provider identity
 * @param completedAt instant O4G completed the full feed successfully
 * @param publishedOfferKeys all provider offers seen in that complete feed
 */
public record CompletedOfferFeed(SourceId providerId, Instant completedAt, Set<OfferKey> publishedOfferKeys) {

    /** Validates that all published identities belong to the completed provider feed. */
    public CompletedOfferFeed {
        Objects.requireNonNull(providerId, "providerId must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");
        publishedOfferKeys = Set.copyOf(Objects.requireNonNull(publishedOfferKeys,
                "publishedOfferKeys must not be null"));
        if (publishedOfferKeys.stream().anyMatch(key -> !providerId.equals(key.providerId()))) {
            throw new IllegalArgumentException("published offer keys must belong to providerId");
        }
    }
}
