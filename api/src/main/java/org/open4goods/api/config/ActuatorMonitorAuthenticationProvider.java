package org.open4goods.api.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;

import org.open4goods.api.config.yml.ActuatorMonitorCredentials;
import org.open4goods.model.RolesConstants;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Authenticates the dedicated Spring Boot Admin identity for api Actuator endpoints.
 *
 * <p>Same provider as ui and front-api (GOU-1393). It is the only HTTP Basic identity of api and is
 * only wired on the Actuator filter chain. It returns {@code null} for every other identity, which
 * then has no provider and is refused. A password mismatch for the configured monitor identity is
 * terminal.
 */
final class ActuatorMonitorAuthenticationProvider implements AuthenticationProvider {

    static final String REQUIRED_ROLE = "ROLE_" + RolesConstants.ACTUATOR_ADMIN_ROLE;

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
