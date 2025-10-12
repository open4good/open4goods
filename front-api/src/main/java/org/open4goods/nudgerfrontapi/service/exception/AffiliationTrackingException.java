package org.open4goods.nudgerfrontapi.service.exception;

/**
 * Exception raised when the affiliation tracking pipeline fails after decoding a valid token.
 */
public class AffiliationTrackingException extends RuntimeException {

    /**
     * Create an exception describing why affiliation tracking failed.
     *
     * @param message human readable explanation of the failure
     * @param cause   root cause thrown by the persistence or serialisation layer
     */
    public AffiliationTrackingException(String message, Throwable cause) {
        super(message, cause);
    }
}
