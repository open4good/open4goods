package org.open4goods.nudgerfrontapi.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates the {@code X-Shared-Token} header against the configured token.
 */
@Component
public class SharedTokenFilter extends OncePerRequestFilter {

    /** Header containing the shared token value. */
    static final String HEADER_NAME = "X-Shared-Token";

    /** Public endpoints that do not require the shared token. */
    public static final String[] PUBLIC_ENDPOINTS = {
        "/",
        "/auth/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    };

    private final SecurityProperties securityProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public SharedTokenFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        for (String pattern : PUBLIC_ENDPOINTS) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!securityProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        String header = request.getHeader(HEADER_NAME);
        if (header == null || !header.equals(securityProperties.getSharedToken())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
