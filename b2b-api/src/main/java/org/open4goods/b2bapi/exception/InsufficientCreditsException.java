package org.open4goods.b2bapi.exception;

/**
 * Raised when an organization cannot reserve credits for a request.
 */
public class InsufficientCreditsException extends B2bApiException {

    public InsufficientCreditsException(final String message) {
        super(ErrorCode.INSUFFICIENT_CREDITS, message);
    }
}
