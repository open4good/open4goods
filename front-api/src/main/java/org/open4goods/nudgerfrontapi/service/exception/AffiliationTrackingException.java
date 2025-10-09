package org.open4goods.nudgerfrontapi.service.exception;

/**
 * Exception raised when the affiliation tracking pipeline fails after decoding a valid token.
 */
public class AffiliationTrackingException extends RuntimeException {

    public AffiliationTrackingException(String message, Throwable cause) {
        super(message, cause);
    }
}
