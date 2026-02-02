package org.open4goods.services.geocode.controller;

/**
 * Raised when no geocode result can be found for the requested city.
 */
public class GeocodeNotFoundException extends RuntimeException
{
    /**
     * Creates a new exception with the given message.
     *
     * @param message error message
     */
    public GeocodeNotFoundException(String message)
    {
        super(message);
    }
}
