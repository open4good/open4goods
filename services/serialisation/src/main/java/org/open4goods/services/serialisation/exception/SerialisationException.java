package org.open4goods.services.serialisation.exception;

/**
 * Custom exception class for handling serialization and deserialization errors.
 */
public class SerialisationException extends Exception {

    private static final long serialVersionUID = 1453990440096206895L;

	/**
     * Constructs a new SerialisationException with the specified detail message.
     *
     * @param message the detail message
     */
    public SerialisationException(String message) {
        super(message);
    }

    /**
     * Constructs a new SerialisationException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public SerialisationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new SerialisationException with the specified cause.
     *
     * @param cause the cause
     */
    public SerialisationException(Throwable cause) {
        super(cause);
    }
}
