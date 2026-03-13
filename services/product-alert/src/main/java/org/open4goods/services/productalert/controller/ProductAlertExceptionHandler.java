package org.open4goods.services.productalert.controller;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps product alert exceptions to RFC 9457 Problem Detail responses.
 */
@RestControllerAdvice
public class ProductAlertExceptionHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductAlertExceptionHandler.class);

    /**
     * Handles request validation failures.
     *
     * @param ex validation exception
     * @return problem detail response
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, ProductAlertBadRequestException.class, IllegalArgumentException.class})
    public ProblemDetail handleBadRequest(Exception ex)
    {
        LOGGER.warn("Invalid product alert request: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Invalid request");
        problemDetail.setDetail(resolveMessage(ex));
        return problemDetail;
    }

    /**
     * Handles missing or invalid internal API keys.
     *
     * @param ex authorization exception
     * @return problem detail response
     */
    @ExceptionHandler(ProductAlertUnauthorizedException.class)
    public ProblemDetail handleUnauthorized(ProductAlertUnauthorizedException ex)
    {
        LOGGER.warn("Unauthorized product alert access: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problemDetail.setTitle("Unauthorized");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    private String resolveMessage(Exception ex)
    {
        if (ex instanceof MethodArgumentNotValidException validationException
                && validationException.getBindingResult().getFieldError() != null)
        {
            return validationException.getBindingResult().getFieldError().getDefaultMessage();
        }
        return ex.getMessage();
    }
}
