package org.open4goods.services.productalert.controller;

/**
 * Raised when a product alert request is invalid.
 */
public class ProductAlertBadRequestException extends RuntimeException
{
    /**
     * Creates a new exception.
     *
     * @param message error message
     */
    public ProductAlertBadRequestException(String message)
    {
        super(message);
    }
}
