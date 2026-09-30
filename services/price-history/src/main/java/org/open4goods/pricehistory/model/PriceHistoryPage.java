package org.open4goods.pricehistory.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A stable ascending page with an opaque continuation. */
public record PriceHistoryPage<T>(List<T> values, Optional<String> nextCursor) {

    /** Defensively copies immutable page contents. */
    public PriceHistoryPage {
        values = List.copyOf(Objects.requireNonNull(values, "values must not be null"));
        nextCursor = Optional.ofNullable(Objects.requireNonNull(nextCursor, "nextCursor must not be null").orElse(null));
    }
}
