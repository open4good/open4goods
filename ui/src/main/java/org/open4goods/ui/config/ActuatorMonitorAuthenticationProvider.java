package org.open4goods.ui.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;

import org.open4goods.model.RolesConstants;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Authenticates the dedicated Spring Boot Admin identity for UI Actuator endpoints.
 *
 * <p>It returns {@code null} for every other identity, leaving interactive authentication to the
 * existing XWiki provider. A password mismatch for the configured identity is terminal so a
 * monitor request is never forwarded to XWiki.
 */
final class ActuatorMonitorAuthenticationProvider implements AuthenticationProvider {

    private static final String REQUIRED_ROLE = "ROLE_" + RolesConstants.ACTUATOR_ADMIN_ROLE;

    private final ActuatorMonitorCredentials credentials;

    ActuatorMonitorAuthenticationProvider(ActuatorMonitorCredentials credentials) {
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
            throw new BadCredentialsException("invalid Actuator monitor credentials");
        }
        return UsernamePasswordAuthenticationToken.authenticated(credentials.getUsername(), null,
                List.of(new SimpleGrantedAuthority(REQUIRED_ROLE)));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
