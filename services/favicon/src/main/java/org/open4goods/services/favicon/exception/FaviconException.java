package org.open4goods.services.favicon.exception;

/**
 * Exception thrown when favicon retrieval fails.
 */
public class FaviconException extends RuntimeException {
    public FaviconException(String message) {
        super(message);
    }
    public FaviconException(String message, Throwable cause) {
        super(message, cause);
    }
}
