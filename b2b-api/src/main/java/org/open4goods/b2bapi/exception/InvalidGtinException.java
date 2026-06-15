package org.open4goods.b2bapi.exception;

/**
 * Raised when a product key is not a valid GTIN.
 */
public class InvalidGtinException extends B2bApiException {

    public InvalidGtinException(final String message) {
        super(ErrorCode.INVALID_GTIN, message);
    }
}
