package org.open4goods.datareference.model.grouping;

/**
 * Indicates that a Git-authored {@link ModelPatternRule} resource does not
 * satisfy its contract.
 */
public final class ModelPatternRuleValidationException extends IllegalArgumentException {

    /**
     * Creates an exception with a concise operator-facing explanation.
     *
     * @param message validation failure explanation
     */
    public ModelPatternRuleValidationException(String message) {
        super(message);
    }

    /**
     * Creates an exception preserving the parser failure that caused it.
     *
     * @param message validation failure explanation
     * @param cause original parser or conversion failure
     */
    public ModelPatternRuleValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
