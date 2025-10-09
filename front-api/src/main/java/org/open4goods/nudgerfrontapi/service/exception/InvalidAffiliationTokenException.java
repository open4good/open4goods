package org.open4goods.nudgerfrontapi.service.exception;

/**
 * Exception thrown when an affiliation token cannot be decoded into a valid contribution vote.
 */
public class InvalidAffiliationTokenException extends RuntimeException {

    public InvalidAffiliationTokenException(String message) {
        super(message);
    }

    public InvalidAffiliationTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
