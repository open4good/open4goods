package org.open4goods.services.reviewgeneration.service;

/**
 * Thrown when an IP exceeds the configured daily AI review generation quota.
 */
public class ReviewGenerationQuotaExceededException extends RuntimeException {

    private final int dailyLimit;

    public ReviewGenerationQuotaExceededException(String ip, int dailyLimit) {
        super("Daily AI review generation limit reached for IP: " + ip);
        this.dailyLimit = dailyLimit;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }
}
