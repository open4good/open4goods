package org.open4goods.b2bapi.exception;

import org.springframework.http.HttpStatus;

/**
 * Error catalog for Product Data API Problem Details.
 */
public enum ErrorCode {

    INVALID_GTIN(HttpStatus.BAD_REQUEST, "invalid-gtin", "Invalid GTIN"),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "invalid-parameter", "Invalid parameter"),
    MISSING_CREDENTIALS(HttpStatus.UNAUTHORIZED, "missing-credentials", "Missing API key"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "invalid-credentials", "Invalid API key"),
    INSUFFICIENT_CREDITS(HttpStatus.PAYMENT_REQUIRED, "insufficient-credits", "Insufficient credits"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "product-not-found", "Product not found"),
    VALIDATION_ERROR(HttpStatus.UNPROCESSABLE_ENTITY, "validation-error", "Validation error"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "rate-limited", "Too many requests"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal error"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "service-unavailable", "Service unavailable");

    private final HttpStatus status;
    private final String slug;
    private final String title;

    ErrorCode(final HttpStatus status, final String slug, final String title) {
        this.status = status;
        this.slug = slug;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String slug() {
        return slug;
    }

    public String title() {
        return title;
    }
}
