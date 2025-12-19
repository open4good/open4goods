package org.open4goods.ui;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global exception handler for handling errors across the entire application.
 * <p>
 * - Handles exceptions thrown by controllers.<br>
 * - Differentiates between client (4xx) and server (5xx) errors.<br>
 * - Provides structured JSON responses for API calls and Thymeleaf error pages for UI requests.<br>
 * - Tracks error occurrences per endpoint for monitoring.<br>
 * - Implements a custom health check indicating server error health status.<br>
 * </p>
 */
@Component
@ControllerAdvice
public class GlobalExceptionHandler implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AtomicInteger serverErrorCount = new AtomicInteger(0);
    private final AtomicInteger clientErrorCount = new AtomicInteger(0);
    private final Map<String, Integer> serverErrorEndpoints = new ConcurrentHashMap<>();
    private final Map<String, Integer> clientErrorEndpoints = new ConcurrentHashMap<>();
    
    // Max number of recorded failed urls (rendered in sb admin client)
    private static final int MAX_ENDPOINTS = 10;
    /**
     * Handles all exceptions and determines the response format (JSON or Thymeleaf error page).
     *
     * @param ex      The exception that was thrown.
     * @param request The HTTP request that triggered the exception.
     * @return JSON response for API requests, or a Thymeleaf error page for web requests.
     */
    @ExceptionHandler(Exception.class)
    public Object handleException(Exception ex, HttpServletRequest request) {
        String endpoint = request.getRequestURI();
        HttpStatus status = determineHttpStatus(ex);

        trackError(status, endpoint);
        logException(status, endpoint, ex);

        return isApiCall(request)
                ? createJsonErrorResponse(status, ex)
                : createThymeleafErrorView(status, request, ex);
    }

    /**
     * Determines the appropriate HTTP status code based on the exception type.
     *
     * @param ex The exception to evaluate.
     * @return The corresponding {@link HttpStatus}.
     */
    private HttpStatus determineHttpStatus(Exception ex) {
        if (ex instanceof ResponseStatusException responseStatusException) {
            return HttpStatus.valueOf(responseStatusException.getStatusCode().value());
        } else if (ex instanceof NoResourceFoundException) {
            return HttpStatus.NOT_FOUND; // 404
        } else if (ex instanceof AuthorizationDeniedException) {
            return HttpStatus.FORBIDDEN; // 403
        }
        return HttpStatus.INTERNAL_SERVER_ERROR; // Default 500
    }

    

    private void trackError(HttpStatus status, String endpoint) {
        if (status.is4xxClientError()) {
            clientErrorCount.incrementAndGet();
            synchronized (clientErrorEndpoints) {
                clientErrorEndpoints.merge(endpoint, 1, Integer::sum);
                if (clientErrorEndpoints.size() > MAX_ENDPOINTS) {
                    removeOldestEntry(clientErrorEndpoints);
                }
            }
        } else if (status.is5xxServerError()) {
            serverErrorCount.incrementAndGet();
            synchronized (serverErrorEndpoints) {
                serverErrorEndpoints.merge(endpoint, 1, Integer::sum);
                if (serverErrorEndpoints.size() > MAX_ENDPOINTS) {
                    removeOldestEntry(serverErrorEndpoints);
                }
            }
        }
    }

    private void removeOldestEntry(Map<String, Integer> map) {
        Iterator<String> it = map.keySet().iterator();
        if (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    /**
     * Logs the exception with appropriate severity based on error type.
     *
     * @param status  The HTTP status of the error.
     * @param endpoint The endpoint where the error occurred.
     * @param ex      The thrown exception.
     */
    private void logException(HttpStatus status, String endpoint, Exception ex) {
        if (status.is5xxServerError()) {
            logger.error("500 Server Error on {}: {}", endpoint, ex.getMessage() != null ? ex.getMessage() : ex.toString(), ex);
        } else {
            logger.warn("Error on {}: {} (HTTP {})", endpoint, ex.getMessage() != null ? ex.getMessage() : ex.toString(), status.value());
        }
    }

    /**
     * Determines if the request is an API call expecting a JSON response.
     *
     * @param request The incoming HTTP request.
     * @return {@code true} if the request expects JSON, otherwise {@code false}.
     */
    private boolean isApiCall(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        return acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * Creates a JSON error response for API requests.
     *
     * @param status The HTTP status of the error.
     * @param ex     The thrown exception.
     * @return A {@link ResponseEntity} containing the JSON error response.
     */
    private ResponseEntity<Map<String, String>> createJsonErrorResponse(HttpStatus status, Exception ex) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "status", String.valueOf(status.value()),
                        "error", status.getReasonPhrase(),
                        "message", ex.getMessage() != null ? ex.getMessage() : ex.toString()
                ));
    }

    /**
     * Creates a Thymeleaf error view for web requests.
     *
     * @param status  The HTTP status of the error.
     * @param request The incoming HTTP request.
     * @param ex      The thrown exception.
     * @return A {@link ModelAndView} for rendering the error page.
     */
    private ModelAndView createThymeleafErrorView(HttpStatus status, HttpServletRequest request, Exception ex) {
        ModelMap model = new ModelMap();
        model.addAttribute("status", status.value());
        model.addAttribute("error", status.getReasonPhrase());
        model.addAttribute("message", ex.getMessage() != null ? ex.getMessage() : ex.toString());
        model.addAttribute("path", request.getRequestURI());

        return new ModelAndView("error/" + status.value(), model, status);
    }

    /**
     * Custom health check for monitoring HTTP errors.
     * Marks the application as "DOWN" if any server errors (500) have been recorded.
     *
     * @return The health status including error details.
     */
    @Override
    public Health health() {
        int total500Errors = serverErrorCount.get();
        int total400Errors = clientErrorCount.get();

        Health.Builder healthBuilder = (total500Errors > 0) ? Health.down() : Health.up();

        return healthBuilder
                .withDetail("total_500_errors", total500Errors)
                .withDetail("total_400_errors", total400Errors)
                .withDetail("server_errors_by_endpoint", Map.copyOf(serverErrorEndpoints))
                .withDetail("client_errors_by_endpoint", Map.copyOf(clientErrorEndpoints))
                .build();
    }
}
