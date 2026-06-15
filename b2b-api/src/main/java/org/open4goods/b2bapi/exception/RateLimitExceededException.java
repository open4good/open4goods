package org.open4goods.b2bapi.exception;

/**
 * Raised when an API key exceeds its configured request rate.
 */
public class RateLimitExceededException extends B2bApiException {

    public RateLimitExceededException(final String message) {
        super(ErrorCode.RATE_LIMITED, message);
    }
}
