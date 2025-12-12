package org.open4goods.nudgerfrontapi.service.exception;

/**
 * Raised when an IP exceeds the allowed number of AI review generation requests
 * for the current day.
 */
public class ReviewGenerationQuotaExceededException extends RuntimeException {

    private final int dailyLimit;

    public ReviewGenerationQuotaExceededException(int dailyLimit) {
        super("Daily AI review generation limit reached");
        this.dailyLimit = dailyLimit;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }
}
