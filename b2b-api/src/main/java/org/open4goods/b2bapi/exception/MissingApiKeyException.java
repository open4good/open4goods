package org.open4goods.b2bapi.exception;

/**
 * Raised when a protected data endpoint is called without an API key.
 */
public class MissingApiKeyException extends B2bApiException {

    public MissingApiKeyException(final String message) {
        super(ErrorCode.MISSING_CREDENTIALS, message);
    }
}
