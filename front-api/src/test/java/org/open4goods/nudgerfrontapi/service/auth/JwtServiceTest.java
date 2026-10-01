package org.open4goods.nudgerfrontapi.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.config.properties.GoogleSsoProperties;
import org.open4goods.nudgerfrontapi.config.properties.SecurityProperties;
import org.open4goods.nudgerfrontapi.service.exception.GoogleIdentityVerificationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Covers the revocation behaviour added for a Google SSO session on refresh: the allowlist must be
 * re-checked on every refresh instead of only at login, while a password session (whose roles are not
 * allowlist-backed) must not be affected by that re-check.
 */
class JwtServiceTest {

    private static final String JWT_SECRET = "jwt-service-test-placeholder-signing-value";

    private GoogleSsoProperties googleSsoProperties;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        SecretKey key = new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();

        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setJwtSecret(JWT_SECRET);

        googleSsoProperties = new GoogleSsoProperties();

        jwtService = new JwtService(encoder, decoder, securityProperties, googleSsoProperties);
    }

    @Test
    void refreshOfSsoSessionFailsOnceEmailIsRemovedFromAllowlist() {
        googleSsoProperties.setRoleAllowlist(Map.of());

        String refresh = jwtService.generateRefreshToken(ssoAuth("owner@example.com", "ROLE_SITEEDITOR"));

        assertThatThrownBy(() -> jwtService.validateRefreshToken(refresh))
                .isInstanceOf(GoogleIdentityVerificationException.class);
    }

    @Test
    void refreshOfSsoSessionGrantsOnlyTheRemainingAllowlistedRoles() {
        googleSsoProperties.setRoleAllowlist(Map.of("owner@example.com", List.of("ROLE_SITEEDITOR")));

        String refresh = jwtService.generateRefreshToken(
                ssoAuth("owner@example.com", "ROLE_SITEEDITOR", "XWIKIADMINGROUP"));

        Authentication refreshed = jwtService.validateRefreshToken(refresh);

        assertThat(authorityNames(refreshed)).containsExactly("ROLE_SITEEDITOR");
    }

    @Test
    void refreshOfPasswordSessionIgnoresTheGoogleSsoAllowlist() {
        googleSsoProperties.setRoleAllowlist(Map.of());

        var passwordAuth = new UsernamePasswordAuthenticationToken("user", "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_SITEEDITOR")));
        String refresh = jwtService.generateRefreshToken(passwordAuth);

        Authentication refreshed = jwtService.validateRefreshToken(refresh);

        assertThat(authorityNames(refreshed)).containsExactly("ROLE_SITEEDITOR");
    }

    private static UsernamePasswordAuthenticationToken ssoAuth(String email, String... roles) {
        var auth = new UsernamePasswordAuthenticationToken(email, "N/A",
                List.of(roles).stream().map(SimpleGrantedAuthority::new).toList());
        auth.setDetails(JwtService.AMR_GOOGLE_SSO);
        return auth;
    }

    private static List<String> authorityNames(Authentication authentication) {
        return authentication.getAuthorities().stream().map(Object::toString).toList();
    }
}
