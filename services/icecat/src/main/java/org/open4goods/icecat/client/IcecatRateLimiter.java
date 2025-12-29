package org.open4goods.icecat.client;

import java.time.Duration;

import org.open4goods.icecat.client.exception.IcecatRateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

/**
 * Rate limiter for Icecat Retailer API.
 * Ensures compliance with the 5 requests/second limit using Guava's token bucket algorithm.
 *
 * <p>Usage:
 * <pre>
 * rateLimiter.acquire(); // Blocks until permit is available
 * // Make API call
 * </pre>
 */
public class IcecatRateLimiter {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcecatRateLimiter.class);
    private static final double DEFAULT_REQUESTS_PER_SECOND = 5.0;

    private final RateLimiter rateLimiter;

    /**
     * Creates a rate limiter with the default limit (5 req/s).
     */
    public IcecatRateLimiter() {
        this(DEFAULT_REQUESTS_PER_SECOND);
    }

    /**
     * Creates a rate limiter with a custom limit.
     *
     * @param requestsPerSecond the maximum requests per second
     */
    public IcecatRateLimiter(double requestsPerSecond) {
        this.rateLimiter = RateLimiter.create(requestsPerSecond);
        LOGGER.info("Icecat rate limiter initialized with {} requests/second", requestsPerSecond);
    }

    /**
     * Acquires a permit, blocking until one is available.
     * Use this before making API calls to ensure rate limit compliance.
     *
     * @return the time in seconds spent waiting for the permit
     */
    public double acquire() {
        double waitTime = rateLimiter.acquire();
        if (waitTime > 0) {
            LOGGER.debug("Rate limiter waited {} seconds for permit", waitTime);
        }
        return waitTime;
    }

    /**
     * Tries to acquire a permit without blocking.
     *
     * @return true if a permit was acquired, false otherwise
     */
    public boolean tryAcquire() {
        return rateLimiter.tryAcquire();
    }

    /**
     * Tries to acquire a permit within the specified timeout.
     *
     * @param timeout the maximum time to wait
     * @return true if a permit was acquired within the timeout
     */
    public boolean tryAcquire(Duration timeout) {
        return rateLimiter.tryAcquire(timeout);
    }

    /**
     * Handles HTTP 429 (Too Many Requests) response by waiting for the specified duration.
     *
     * @param retryAfter the duration to wait before retrying
     * @throws IcecatRateLimitException if interrupted while waiting
     */
    public void handleRateLimitResponse(Duration retryAfter) {
        LOGGER.warn("Received rate limit response, waiting {} seconds", retryAfter.getSeconds());
        try {
            Thread.sleep(retryAfter.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IcecatRateLimitException(
                    "Interrupted while waiting for rate limit",
                    null,
                    retryAfter
            );
        }
    }

    /**
     * Gets the current rate limit in requests per second.
     *
     * @return the rate limit
     */
    public double getRate() {
        return rateLimiter.getRate();
    }
}
