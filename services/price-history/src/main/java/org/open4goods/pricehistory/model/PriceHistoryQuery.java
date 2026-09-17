package org.open4goods.pricehistory.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.SourceId;

/**
 * Provider-price query with inclusive UTC start and exclusive UTC end.
 *
 * @param from inclusive UTC instant
 * @param to exclusive UTC instant
 * @param requestedGranularity explicit source selection, or empty for the safe default
 * @param gtin optional product filter
 * @param providerId optional provider filter
 * @param condition optional condition filter
 * @param currency optional ISO 4217 currency filter
 * @param cursor opaque continuation returned by a previous page
 * @param pageSize bounded number of entries
 * @param includeLegacy whether the separate neutral legacy shape is requested
 */
public record PriceHistoryQuery(
        Instant from,
        Instant to,
        Optional<PriceHistoryGranularity> requestedGranularity,
        Optional<Gtin> gtin,
        Optional<SourceId> providerId,
        Optional<OfferCondition> condition,
        Optional<Currency> currency,
        Optional<String> cursor,
        int pageSize,
        boolean includeLegacy) {

    private static final int MAX_PAGE_SIZE = 500;

    /** Validates range semantics and request bounds. */
    public PriceHistoryQuery {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to in a half-open range");
        }
        requestedGranularity = copy(requestedGranularity, "requestedGranularity must not be null");
        gtin = copy(gtin, "gtin must not be null");
        providerId = copy(providerId, "providerId must not be null");
        condition = copy(condition, "condition must not be null");
        currency = copy(currency, "currency must not be null");
        cursor = copy(cursor, "cursor must not be null");
        if (cursor.isPresent() && cursor.get().isBlank()) {
            throw new IllegalArgumentException("cursor must be opaque and nonblank when present");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    /** Selects daily data by default for ranges over 31 days. */
    public PriceHistoryGranularity effectiveGranularity() {
        return requestedGranularity.orElse(Duration.between(from, to).compareTo(Duration.ofDays(31)) > 0
                ? PriceHistoryGranularity.DAY : PriceHistoryGranularity.CHANGE);
    }

    private static <T> Optional<T> copy(Optional<T> optional, String message) {
        return Optional.ofNullable(Objects.requireNonNull(optional, message).orElse(null));
    }
}
