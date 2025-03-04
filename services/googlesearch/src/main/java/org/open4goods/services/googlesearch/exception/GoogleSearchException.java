package org.open4goods.services.googlesearch.exception;

/**
 * Exception thrown when an error occurs while interacting with the Google Custom Search API.
 */
public class GoogleSearchException extends RuntimeException {

    /**
     * Constructs a new GoogleSearchException with the specified detail message.
     *
     * @param message the detail message
     */
    public GoogleSearchException(String message) {
        super(message);
    }

    /**
     * Constructs a new GoogleSearchException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public GoogleSearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
