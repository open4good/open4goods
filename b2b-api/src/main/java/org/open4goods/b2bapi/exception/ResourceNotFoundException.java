package org.open4goods.b2bapi.exception;

/**
 * Raised when a requested B2B resource does not exist.
 */
public class ResourceNotFoundException extends B2bApiException {

    public ResourceNotFoundException(final String message) {
        super(ErrorCode.PRODUCT_NOT_FOUND, message);
    }
}
