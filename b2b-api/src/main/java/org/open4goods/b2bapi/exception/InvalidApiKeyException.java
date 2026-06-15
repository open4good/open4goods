package org.open4goods.b2bapi.exception;

/**
 * Raised when API-key authentication fails.
 */
public class InvalidApiKeyException extends B2bApiException {

    public InvalidApiKeyException(final String message) {
        super(ErrorCode.INVALID_CREDENTIALS, message);
    }
}
