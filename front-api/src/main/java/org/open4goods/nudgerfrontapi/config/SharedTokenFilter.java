package org.open4goods.nudgerfrontapi.config;

import java.io.IOException;
import java.util.List;

import org.open4goods.model.RolesConstants;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter injecting ROLE_MACHINE for non public requests if the {@code X-Shared-Token} header matches expected key
 */
@Component
public class SharedTokenFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Shared-Token";

    private final SecurityProperties securityProperties;

    public SharedTokenFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;

    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {


        String expected = securityProperties.getSharedToken();
        String actual = request.getHeader(HEADER_NAME);

        if (StringUtils.hasText(expected) && expected.equals(actual)) {
            // Injecter ROLE_FRONTEND
            SimpleGrantedAuthority frontendRole = new SimpleGrantedAuthority(RolesConstants.ROLE_FRONTEND);
            Authentication auth = new UsernamePasswordAuthenticationToken(
                "shared-token-user",  // ou null si pas d'identifiant spécifique
                null,
                List.of(frontendRole)
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }


}
