package org.open4goods.nudgerfrontapi.controller.advice;

import java.util.concurrent.atomic.AtomicInteger;

import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@RestControllerAdvice
/**
 * RFC 9457 Compliant Exception to Problem Detail mapping
 * TODO : Could count to feed actuator metrics / healthchecks
 */
public class GlobalExceptionHandler implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AtomicInteger clientErrors = new AtomicInteger();
    private final AtomicInteger serverErrors = new AtomicInteger();
    private final Counter serverErrorsCounter;

    public GlobalExceptionHandler(MeterRegistry meterRegistry) {
        this.serverErrorsCounter = meterRegistry.counter("server.errors");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        clientErrors.incrementAndGet();
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Resource Not Found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception ex) {
        serverErrors.incrementAndGet();
        serverErrorsCounter.increment();
        log.error("Unhandled exception", ex);
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Internal Server Error");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @Override
    public Health health() {
        return Health.up()
            .withDetail("clientErrors", clientErrors.get())
            .withDetail("serverErrors", serverErrors.get())
            .build();
    }
}
