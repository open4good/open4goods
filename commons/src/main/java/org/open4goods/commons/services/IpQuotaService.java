package org.open4goods.commons.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * Service generic for managing quotas/limits per IP address.
 * Allows to count actions per IP per day and check limits.
 */
@Service
public class IpQuotaService {

    private final Cache<String, Integer> quotas;
    private final Clock clock;

    private static final Duration DEFAULT_WINDOW = Duration.ofDays(1);

    public IpQuotaService() {
        this(Clock.systemUTC());
    }

    /**
     * Create a new quota service using the provided clock (for testing).
     *
     * @param clock clock to use for window calculations
     */
    IpQuotaService(Clock clock) {
        this.clock = clock;
        // We keep data for 48 hours to ensure we cover "yesterday" if needed for debugging or edge cases,
        // but the key logic relies on LocalDate.now() so it essentially resets every day.
        this.quotas = Caffeine.newBuilder()
                .expireAfterWrite(48, TimeUnit.HOURS)
                .build();
    }

    /**
     * Generates a key unique for the action, IP, and current date.
     */
    private String key(String action, String ip) {
        return action + ":" + ip + ":" + LocalDate.now().toString();
    }

    /**
     * Generates a key unique for the action, IP, and current time bucket.
     *
     * @param action identifier of the action
     * @param ip client IP
     * @param window duration of the quota window
     * @return key scoped to the time window bucket
     */
    private String key(String action, String ip, Duration window) {
        Duration normalizedWindow = normalizeWindow(window);
        long windowSeconds = normalizedWindow.getSeconds();
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("Quota window must be greater than zero");
        }
        Instant now = clock.instant();
        long bucket = Math.floorDiv(now.getEpochSecond(), windowSeconds);
        return action + ":" + ip + ":window:" + bucket;
    }

    /**
     * Resolve the effective window duration to apply.
     *
     * @param window configured duration
     * @return non-null duration used for bucket computation
     */
    private Duration normalizeWindow(Duration window) {
        return window == null ? DEFAULT_WINDOW : window;
    }

    /**
     * Returns the number of times the action was performed by this IP today.
     * @param action Identifier of the action (e.g. "VOTE")
     * @param ip Client IP
     * @return Usage count
     */
    public int getUsage(String action, String ip) {
        Integer val = quotas.getIfPresent(key(action, ip));
        return val == null ? 0 : val;
    }

    /**
     * Returns the remaining number of allowed actions for this IP today.
     * @param action Identifier of the action
     * @param ip Client IP
     * @param maxLimit Maximum allowed actions per day
     * @return Remaining count (>= 0)
     */
    public int getRemaining(String action, String ip, int maxLimit) {
        return Math.max(0, maxLimit - getUsage(action, ip));
    }

    /**
     * Checks if the IP is allowed to perform the action (usage < maxLimit).
     * @param action Identifier of the action
     * @param ip Client IP
     * @param maxLimit Maximum allowed actions per day
     * @return true if allowed
     */
    public boolean isAllowed(String action, String ip, int maxLimit) {
        return getUsage(action, ip) < maxLimit;
    }

    /**
     * Returns the number of times the action was performed by this IP in the current time window.
     *
     * @param action Identifier of the action (e.g. "REVIEW_GENERATION")
     * @param ip Client IP
     * @param window duration of the quota window
     * @return Usage count within the window
     */
    public int getUsage(String action, String ip, Duration window) {
        Integer val = quotas.getIfPresent(key(action, ip, window));
        return val == null ? 0 : val;
    }

    /**
     * Returns the remaining number of allowed actions for this IP in the time window.
     *
     * @param action Identifier of the action
     * @param ip Client IP
     * @param maxLimit Maximum allowed actions per window
     * @param window duration of the quota window
     * @return Remaining count (>= 0)
     */
    public int getRemaining(String action, String ip, int maxLimit, Duration window) {
        return Math.max(0, maxLimit - getUsage(action, ip, window));
    }

    /**
     * Checks if the IP is allowed to perform the action (usage < maxLimit) within the window.
     *
     * @param action Identifier of the action
     * @param ip Client IP
     * @param maxLimit Maximum allowed actions per window
     * @param window duration of the quota window
     * @return true if allowed
     */
    public boolean isAllowed(String action, String ip, int maxLimit, Duration window) {
        return getUsage(action, ip, window) < maxLimit;
    }

    /**
     * Increments the usage count for the action and IP for today.
     * @param action Identifier of the action
     * @param ip Client IP
     * @return The new total usage count
     */
    public int increment(String action, String ip) {
        String k = key(action, ip);
        return quotas.asMap().compute(k, (key, value) -> (value == null ? 0 : value) + 1);
    }

    /**
     * Increments the usage count for the action and IP in the current time window.
     *
     * @param action Identifier of the action
     * @param ip Client IP
     * @param window duration of the quota window
     * @return The new total usage count
     */
    public int increment(String action, String ip, Duration window) {
        String k = key(action, ip, window);
        return quotas.asMap().compute(k, (key, value) -> (value == null ? 0 : value) + 1);
    }
}
