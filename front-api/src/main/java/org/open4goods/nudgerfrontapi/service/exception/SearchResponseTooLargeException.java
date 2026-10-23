package org.open4goods.nudgerfrontapi.service.exception;

/**
 * Raised when an Elasticsearch search response exceeds the HTTP client buffer even after
 * the server-side bounds on page size and aggregations have been applied.
 */
public class SearchResponseTooLargeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SearchResponseTooLargeException(String message, Throwable cause) {
        super(message, cause);
    }
}
