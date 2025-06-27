package org.open4goods.api.model;

import org.open4goods.model.price.Price;

/**
 * Search result item returned by various endpoints.
 */
public record SearchResult(
        String brand,
        String brandUid,
        Long dateIndexed,
        Price price,
        String url) {
}

