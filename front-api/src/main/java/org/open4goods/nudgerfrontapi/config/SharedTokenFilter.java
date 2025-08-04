package org.open4goods.nudgerfrontapi.config;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Filter verifying the {@code X-Shared-Token} header for non public requests.
 */
@Component
public class SharedTokenFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Shared-Token";

    private final SecurityProperties securityProperties;
    private final List<RequestMatcher> publicMatchers;

    public SharedTokenFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        this.publicMatchers = List.of(
                new AntPathRequestMatcher("/"),
                new AntPathRequestMatcher("/auth/**"),
                new AntPathRequestMatcher("/actuator/**"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isPublic(request) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String expected = securityProperties.getSharedToken();
        String actual = request.getHeader(HEADER_NAME);

        if (StringUtils.hasText(expected) && expected.equals(actual)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private boolean isPublic(HttpServletRequest request) {
        return publicMatchers.stream().anyMatch(m -> m.matches(request));
    }
}
