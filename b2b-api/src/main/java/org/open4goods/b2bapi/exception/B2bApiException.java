package org.open4goods.b2bapi.exception;

/**
 * Base exception for documented Product Data API failures.
 */
public class B2bApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public B2bApiException(final ErrorCode errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public B2bApiException(final ErrorCode errorCode, final String message, final Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
