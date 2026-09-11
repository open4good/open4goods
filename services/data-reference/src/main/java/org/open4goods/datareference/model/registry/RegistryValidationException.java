package org.open4goods.datareference.model.registry;

/**
 * Indicates that a Git-authored registry resource does not satisfy its contract.
 */
public final class RegistryValidationException extends IllegalArgumentException {

    /**
     * Creates an exception with a concise operator-facing explanation.
     *
     * @param message validation failure explanation
     */
    public RegistryValidationException(String message) {
        super(message);
    }

    /**
     * Creates an exception preserving the parser failure that caused it.
     *
     * @param message validation failure explanation
     * @param cause original parser or conversion failure
     */
    public RegistryValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
