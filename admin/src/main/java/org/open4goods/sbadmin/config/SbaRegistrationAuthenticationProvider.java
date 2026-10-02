package org.open4goods.sbadmin.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Authenticates the dedicated local credential used by monitored SBA clients.
 *
 * <p>This provider does not replace interactive XWiki authentication. It activates only when
 * both configuration values are present and grants the role expected by the SBA server.
 */
final class SbaRegistrationAuthenticationProvider implements AuthenticationProvider {

    private static final String REQUIRED_ROLE = "ROLE_XWIKIADMINGROUP";

    private final SbaRegistrationCredentials credentials;

    SbaRegistrationAuthenticationProvider(SbaRegistrationCredentials credentials) {
        this.credentials = Objects.requireNonNull(credentials, "credentials must not be null");
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (!credentials.isConfigured() || !credentials.getUsername().equals(authentication.getName())) {
            return null;
        }
        String suppliedPassword = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();
        if (!MessageDigest.isEqual(credentials.getPassword().getBytes(StandardCharsets.UTF_8),
                suppliedPassword.getBytes(StandardCharsets.UTF_8))) {
            throw new BadCredentialsException("invalid Spring Boot Admin registration credentials");
        }
        return UsernamePasswordAuthenticationToken.authenticated(credentials.getUsername(), null,
                List.of(new SimpleGrantedAuthority(REQUIRED_ROLE)));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
