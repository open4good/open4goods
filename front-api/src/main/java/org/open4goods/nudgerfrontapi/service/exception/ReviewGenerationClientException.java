package org.open4goods.nudgerfrontapi.service.exception;

import org.springframework.http.HttpStatusCode;

/**
 * Exception raised when the back-office review generation endpoints cannot be reached
 * or return an error payload.
 */
public class ReviewGenerationClientException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public ReviewGenerationClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public ReviewGenerationClientException(String message, HttpStatusCode statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
