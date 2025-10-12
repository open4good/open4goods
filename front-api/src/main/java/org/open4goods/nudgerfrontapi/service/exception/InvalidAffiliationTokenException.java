package org.open4goods.nudgerfrontapi.service.exception;

/**
 * Exception thrown when an affiliation token cannot be decoded into a valid contribution vote.
 */
public class InvalidAffiliationTokenException extends RuntimeException {

    /**
     * Create an exception describing why an affiliation token is invalid.
     *
     * @param message human readable explanation of the validation failure
     */
    public InvalidAffiliationTokenException(String message) {
        super(message);
    }

    /**
     * Create an exception describing why an affiliation token is invalid.
     *
     * @param message human readable explanation of the validation failure
     * @param cause   underlying cause of the failure
     */
    public InvalidAffiliationTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
