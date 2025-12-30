package org.open4goods.services.exposeddocs.controller;

import org.open4goods.services.exposeddocs.service.ExposedDocsNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps service exceptions to RFC 9457 Problem Detail payloads.
 */
@RestControllerAdvice
public class ExposedDocsExceptionHandler
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ExposedDocsExceptionHandler.class);

    /**
     * Handles missing resource errors.
     *
     * @param ex exception raised by the service
     * @return problem detail response
     */
    @ExceptionHandler(ExposedDocsNotFoundException.class)
    public ProblemDetail handleNotFound(ExposedDocsNotFoundException ex)
    {
        LOGGER.warn("Exposed docs resource not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Resource not found");
        pd.setDetail(ex.getMessage());
        return pd;
    }
}
