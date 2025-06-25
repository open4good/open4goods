package org.open4goods.nudgerfrontapi.ratelimit;

/**
 * Thrown when a request exceeds configured rate limits.
 */
public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}
