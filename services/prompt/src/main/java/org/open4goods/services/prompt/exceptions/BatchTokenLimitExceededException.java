package org.open4goods.services.prompt.exceptions;

/**
 * Exception thrown when the total token count of a batch exceeds the allowed limit.
 */
public class BatchTokenLimitExceededException extends RuntimeException {
    public BatchTokenLimitExceededException(String message) {
        super(message);
    }
}
