package org.open4goods.b2bapi.controller.advice;

import java.net.URI;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.open4goods.b2bapi.exception.B2bApiException;
import org.open4goods.b2bapi.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts Product Data API failures to RFC 9457 Problem Details.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(B2bApiException.class)
    ProblemDetail handleB2bApiException(final B2bApiException exception, final HttpServletRequest request) {
        return toProblem(exception.errorCode(), exception.getMessage(), request);
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class })
    ProblemDetail handleValidationException(final Exception exception, final HttpServletRequest request) {
        return toProblem(ErrorCode.VALIDATION_ERROR, exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnhandledException(final Exception exception, final HttpServletRequest request) {
        final String requestId = Optional.ofNullable(request.getHeader("X-Request-Id")).orElse("unavailable");
        LOGGER.error("Unhandled Product Data API failure requestId={}", requestId, exception);
        return toProblem(ErrorCode.INTERNAL_ERROR, "Unexpected server error.", request);
    }

    private ProblemDetail toProblem(final ErrorCode errorCode, final String detail, final HttpServletRequest request) {
        final HttpStatus status = errorCode.status();
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://product-data-api.com/problems/" + errorCode.slug()));
        problem.setTitle(errorCode.title());
        problem.setInstance(URI.create(request.getRequestURI()));
        Optional.ofNullable(request.getHeader("X-Request-Id")).ifPresent(requestId -> problem.setProperty("requestId", requestId));
        return problem;
    }
}
