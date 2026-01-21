package org.open4goods.services.prompt.exceptions;

/**
 * Exception thrown when a batch prompt job fails on the provider side.
 */
public class BatchJobFailedException extends RuntimeException {

    public BatchJobFailedException(String message) {
        super(message);
    }

    public BatchJobFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
