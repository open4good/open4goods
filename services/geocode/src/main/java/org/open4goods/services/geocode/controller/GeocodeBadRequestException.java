package org.open4goods.services.geocode.controller;

/**
 * Raised when a geocode request is missing required parameters.
 */
public class GeocodeBadRequestException extends RuntimeException
{
    /**
     * Creates a new exception with the given message.
     *
     * @param message error message
     */
    public GeocodeBadRequestException(String message)
    {
        super(message);
    }
}
