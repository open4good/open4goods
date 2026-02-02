package org.open4goods.services.geocode.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps geocode exceptions to RFC 9457 Problem Detail responses.
 */
@RestControllerAdvice
public class GeocodeExceptionHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GeocodeExceptionHandler.class);

    /**
     * Handles missing parameter errors.
     *
     * @param ex exception raised by the controller
     * @return problem detail response
     */
    @ExceptionHandler(GeocodeBadRequestException.class)
    public ProblemDetail handleBadRequest(GeocodeBadRequestException ex)
    {
        LOGGER.warn("Bad geocode request: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Invalid request");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    /**
     * Handles missing city lookups.
     *
     * @param ex exception raised by the service
     * @return problem detail response
     */
    @ExceptionHandler(GeocodeNotFoundException.class)
    public ProblemDetail handleNotFound(GeocodeNotFoundException ex)
    {
        LOGGER.warn("Geocode result not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("City not found");
        pd.setDetail(ex.getMessage());
        return pd;
    }
}
