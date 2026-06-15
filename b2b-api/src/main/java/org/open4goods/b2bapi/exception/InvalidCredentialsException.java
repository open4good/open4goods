package org.open4goods.b2bapi.exception;

/**
 * Raised when a dashboard or API credential cannot be verified.
 */
public class InvalidCredentialsException extends B2bApiException {

    public InvalidCredentialsException(final String message) {
        super(ErrorCode.INVALID_CREDENTIALS, message);
    }

    public InvalidCredentialsException(final String message, final Throwable cause) {
        super(ErrorCode.INVALID_CREDENTIALS, message, cause);
    }
}
