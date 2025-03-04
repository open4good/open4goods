package org.open4goods.evaluation.exception;

/**
 * Exception thrown when a Thymeleaf template evaluation fails due to unresolved variables.
 */
public class TemplateEvaluationException extends RuntimeException {

    /**
     * Constructs a new TemplateEvaluationException with the specified detail message.
     *
     * @param message the detail message.
     */
    public TemplateEvaluationException(String message) {
        super(message);
    }

    /**
     * Constructs a new TemplateEvaluationException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause.
     */
    public TemplateEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
