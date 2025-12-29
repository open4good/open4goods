package org.open4goods.icecat.client.exception;

import java.time.Duration;

/**
 * Exception thrown when the Icecat API returns HTTP 429 (Too Many Requests).
 * Contains the Retry-After duration to respect rate limiting.
 */
public class IcecatRateLimitException extends IcecatApiException {

    private final Duration retryAfter;

    /**
     * Constructor for IcecatRateLimitException.
     *
     * @param url        the URL that triggered rate limiting
     * @param retryAfter the duration to wait before retrying
     */
    public IcecatRateLimitException(String url, Duration retryAfter) {
        super("Rate limit exceeded. Retry after " + retryAfter.getSeconds() + " seconds",
              429, null, url);
        this.retryAfter = retryAfter;
    }

    /**
     * Constructor for IcecatRateLimitException with custom message.
     *
     * @param message    the error message
     * @param url        the URL that triggered rate limiting
     * @param retryAfter the duration to wait before retrying
     */
    public IcecatRateLimitException(String message, String url, Duration retryAfter) {
        super(message, 429, null, url);
        this.retryAfter = retryAfter;
    }

    /**
     * Returns the duration to wait before retrying.
     *
     * @return the retry-after duration
     */
    public Duration getRetryAfter() {
        return retryAfter;
    }
}
