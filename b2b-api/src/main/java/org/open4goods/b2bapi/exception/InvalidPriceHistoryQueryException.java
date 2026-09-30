package org.open4goods.b2bapi.exception;

/**
 * Raised when a price-history request's date range, page size, or cursor is invalid.
 *
 * <p>Wraps the {@link org.open4goods.pricehistory.model.PriceHistoryQuery} compact-constructor
 * validation (the single source of truth for the GOU-100 {@code 1..500} page-size bound) into a
 * documented RFC 9457 Product Data API failure.
 */
public class InvalidPriceHistoryQueryException extends B2bApiException {

    public InvalidPriceHistoryQueryException(final String message) {
        this(ErrorCode.INVALID_PARAMETER, message);
    }

    public InvalidPriceHistoryQueryException(final ErrorCode errorCode, final String message) {
        super(errorCode, message);
    }
}
