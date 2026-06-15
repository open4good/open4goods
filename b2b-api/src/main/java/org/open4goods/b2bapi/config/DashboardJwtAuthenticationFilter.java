package org.open4goods.b2bapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.open4goods.b2bapi.exception.InvalidCredentialsException;
import org.open4goods.b2bapi.service.AuthTokenResolver;
import org.open4goods.b2bapi.service.DashboardAuthenticationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates dashboard/admin requests from access JWT bearer headers or cookies.
 */
@Component
@ConditionalOnBean(name = "entityManagerFactory")
public class DashboardJwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthTokenResolver authTokenResolver;
    private final DashboardAuthenticationService dashboardAuthenticationService;

    public DashboardJwtAuthenticationFilter(
            final AuthTokenResolver authTokenResolver,
            final DashboardAuthenticationService dashboardAuthenticationService) {
        this.authTokenResolver = authTokenResolver;
        this.dashboardAuthenticationService = dashboardAuthenticationService;
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        final String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/api/v1/products/")
                || path.equals("/actuator/health")
                || path.startsWith("/v3/api-docs/")
                || path.startsWith("/swagger-ui/")
                || path.equals("/swagger-ui.html")
                || path.equals("/redoc")
                || path.equals("/scalar");
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                final String accessToken = authTokenResolver.resolveAccessToken(request);
                SecurityContextHolder.getContext().setAuthentication(
                        dashboardAuthenticationService.authenticate(accessToken));
            } catch (final InvalidCredentialsException | IllegalArgumentException exception) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
