package org.open4goods.b2bapi.exception;

/**
 * Raised when Redis is required for a metered operation but unavailable.
 */
public class RedisUnavailableException extends B2bApiException {

    public RedisUnavailableException(final String message) {
        super(ErrorCode.SERVICE_UNAVAILABLE, message);
    }
}
