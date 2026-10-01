package org.open4goods.nudgerfrontapi.service.exception;

/**
 * Raised when a Google ID token fails verification or resolves to an email
 * absent from the environment role allowlist.
 */
public class GoogleIdentityVerificationException extends RuntimeException {

    public GoogleIdentityVerificationException(String message) {
        super(message);
    }

    public GoogleIdentityVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
