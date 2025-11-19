package org.open4goods.nudgerfrontapi.controller.advice;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.nudgerfrontapi.service.exception.InvalidAffiliationTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
/**
 * RFC 9457 Compliant Exception to Problem Detail mapping.
 * Tracks exception occurrences via Micrometer metrics for observability and
 * health monitoring.
 */
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Counter notFoundCounter;
    private final Counter accessDeniedCounter;
    private final Counter invalidTokenCounter;
    private final Counter unhandledExceptionCounter;

    public GlobalExceptionHandler(MeterRegistry meterRegistry) {
        this.notFoundCounter = Counter.builder("api.exceptions")
                .tag("type", "resource_not_found")
                .tag("status", String.valueOf(HttpStatus.NOT_FOUND.value()))
                .description("Count of resource not found exceptions")
                .register(meterRegistry);

        this.accessDeniedCounter = Counter.builder("api.exceptions")
                .tag("type", "access_denied")
                .tag("status", String.valueOf(HttpStatus.FORBIDDEN.value()))
                .description("Count of access denied exceptions")
                .register(meterRegistry);

        this.invalidTokenCounter = Counter.builder("api.exceptions")
                .tag("type", "invalid_affiliation_token")
                .tag("status", String.valueOf(HttpStatus.BAD_REQUEST.value()))
                .description("Count of invalid affiliation token exceptions")
                .register(meterRegistry);

        this.unhandledExceptionCounter = Counter.builder("api.exceptions")
                .tag("type", "unhandled")
                .tag("status", String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .description("Count of unhandled exceptions")
                .register(meterRegistry);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        notFoundCounter.increment();
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Not Found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler({ AuthorizationDeniedException.class, AccessDeniedException.class })
    public ProblemDetail handleAccessDenied(Exception ex) {
        accessDeniedCounter.increment();
        log.warn("Access denied: {}", ex.getMessage(), ex);
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setTitle("Forbidden");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(InvalidAffiliationTokenException.class)
    public ProblemDetail handleInvalidAffiliationToken(InvalidAffiliationTokenException exception) {
        invalidTokenCounter.increment();
        log.warn("Invalid affiliation token: {}", exception.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Invalid affiliation token");
        pd.setDetail(exception.getMessage());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception ex) {
        unhandledExceptionCounter.increment();
        log.error("Unhandled exception", ex);
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Internal Server Error");
        pd.setDetail(ex.getMessage());
        return pd;
    }
}
