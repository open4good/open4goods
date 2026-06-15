package org.open4goods.b2bapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.open4goods.b2bapi.exception.InvalidApiKeyException;
import org.open4goods.b2bapi.service.ApiKeyAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates external product endpoint calls with opaque {@code pdapi_} bearer keys.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ApiKeyAuthenticationService apiKeyAuthenticationService;

    @Autowired
    public ApiKeyAuthFilter(final ObjectProvider<ApiKeyAuthenticationService> apiKeyAuthenticationService) {
        this(apiKeyAuthenticationService.getIfAvailable());
    }

    ApiKeyAuthFilter(final ApiKeyAuthenticationService apiKeyAuthenticationService) {
        this.apiKeyAuthenticationService = apiKeyAuthenticationService;
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/products/");
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        if (apiKeyAuthenticationService == null) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        try {
            final String clearKey = authorization.substring(BEARER_PREFIX.length()).trim();
            SecurityContextHolder.getContext().setAuthentication(apiKeyAuthenticationService.authenticate(clearKey));
        } catch (final InvalidApiKeyException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        filterChain.doFilter(request, response);
    }
}
