package org.open4goods.services.googleindexation.exception;

/**
 * Exception raised when the Google Indexing API cannot be invoked.
 */
public class GoogleIndexationException extends RuntimeException {

    /**
     * Create a new exception with a message.
     *
     * @param message error message
     */
    public GoogleIndexationException(String message) {
        super(message);
    }

    /**
     * Create a new exception with a message and cause.
     *
     * @param message error message
     * @param cause root cause
     */
    public GoogleIndexationException(String message, Throwable cause) {
        super(message, cause);
    }
}
