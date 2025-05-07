package org.open4goods.services.feedback.exception;

/**
 * Thrown when an IP has reached the maximum number of allowed votes for the day.
 */
public class VotingLimitExceededException extends RuntimeException {
    public VotingLimitExceededException(String message) {
        super(message);
    }
}
