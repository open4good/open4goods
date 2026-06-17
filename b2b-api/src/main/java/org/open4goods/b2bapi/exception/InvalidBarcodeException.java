package org.open4goods.b2bapi.exception;

/**
 * Raised when barcode parameters or data are invalid.
 */
public class InvalidBarcodeException extends B2bApiException {

    public InvalidBarcodeException(final String message) {
        super(ErrorCode.INVALID_PARAMETER, message);
    }

    public InvalidBarcodeException(final String message, final Throwable cause) {
        super(ErrorCode.INVALID_PARAMETER, message, cause);
    }
}
