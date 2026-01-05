package org.open4goods.nudgerfrontapi.service.exception;

/**
 * Exception raised when an IP exceeds the allowed number of review generations.
 */
public class ReviewGenerationLimitExceededException extends RuntimeException {

    /**
     * Create a new exception with a descriptive message.
     *
     * @param message error message describing the quota violation
     */
    public ReviewGenerationLimitExceededException(String message) {
        super(message);
    }
}
