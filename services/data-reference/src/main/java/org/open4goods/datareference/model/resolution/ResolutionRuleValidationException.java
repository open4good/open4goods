package org.open4goods.datareference.model.resolution;

/**
 * Indicates that a Git-authored resolution-rule resource does not satisfy its contract.
 */
public final class ResolutionRuleValidationException extends IllegalArgumentException {

    /**
     * Creates an exception with a concise operator-facing explanation.
     *
     * @param message validation failure explanation
     */
    public ResolutionRuleValidationException(String message) {
        super(message);
    }

    /**
     * Creates an exception preserving the parser failure that caused it.
     *
     * @param message validation failure explanation
     * @param cause original parser or conversion failure
     */
    public ResolutionRuleValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
