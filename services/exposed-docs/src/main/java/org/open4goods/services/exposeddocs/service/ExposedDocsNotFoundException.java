package org.open4goods.services.exposeddocs.service;

/**
 * Raised when an exposed documentation resource or category cannot be resolved.
 */
public class ExposedDocsNotFoundException extends RuntimeException
{

    /**
     * Creates a not-found exception with a message.
     *
     * @param message error message
     */
    public ExposedDocsNotFoundException(String message)
    {
        super(message);
    }
}
