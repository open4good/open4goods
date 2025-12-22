package org.open4goods.commons.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * Service generic for managing quotas/limits per IP address.
 * Allows to count actions per IP per day and check limits.
 */
@Service
public class IpQuotaService {

    private final Cache<String, Integer> quotas;

    public IpQuotaService() {
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
     * Increments the usage count for the action and IP for today.
     * @param action Identifier of the action
     * @param ip Client IP
     * @return The new total usage count
     */
    public int increment(String action, String ip) {
        String k = key(action, ip);
        return quotas.asMap().compute(k, (key, value) -> (value == null ? 0 : value) + 1);
    }
}
