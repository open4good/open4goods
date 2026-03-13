package org.open4goods.services.productalert.controller;

/**
 * Raised when the internal API key check fails.
 */
public class ProductAlertUnauthorizedException extends RuntimeException
{
    /**
     * Creates a new exception.
     *
     * @param message error message
     */
    public ProductAlertUnauthorizedException(String message)
    {
        super(message);
    }
}
