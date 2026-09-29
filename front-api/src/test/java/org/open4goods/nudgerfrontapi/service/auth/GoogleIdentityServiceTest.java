package org.open4goods.nudgerfrontapi.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.config.properties.SecurityProperties;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** Tests deny-by-default, case-insensitive email role assignment. */
class GoogleIdentityServiceTest {

    @Test
    void resolvesOnlyAnExplicitlyAssignedEmail() {
        GoogleIdentityService service = new GoogleIdentityService(properties());

        assertThat(service.identityFor("OWNER@example.test").roles()).containsExactly("ROLE_ADMIN", "ROLE_EDITOR");
        assertThatThrownBy(() -> service.identityFor("someone@example.test"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rejectsAbsentEmailAndEmptyAssignments() {
        GoogleIdentityService service = new GoogleIdentityService(properties());

        assertThatThrownBy(() -> service.identityFor(null)).isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> service.identityFor("editor@example.test")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rejectsRolesOutsideTheExplicitGoogleRoleContract() {
        SecurityProperties properties = properties();
        properties.getGoogle().getAllowedRoles().getFirst().setRoles(List.of("ROLE_FRONTEND"));

        assertThatThrownBy(() -> new GoogleIdentityService(properties).identityFor("owner@example.test"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rejectsTamperedTokensUnexpectedAudiencesAndUnverifiedEmails() {
        SecurityProperties properties = properties();
        properties.getGoogle().setAudience("google-client");
        JwtDecoder tampered = token -> { throw new BadJwtException("signature validation failed"); };
        assertThatThrownBy(() -> new GoogleIdentityService(properties, ignored -> tampered).verify("tampered"))
                .isInstanceOf(BadJwtException.class);

        assertThatThrownBy(() -> serviceFor(properties, List.of("another-client"), true).verify("valid"))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> serviceFor(properties, List.of("google-client"), false).verify("valid"))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> serviceFor(properties, List.of("google-client"), true, Instant.now().minusSeconds(1))
                .verify("valid")).isInstanceOf(BadCredentialsException.class);
    }

    private GoogleIdentityService serviceFor(SecurityProperties properties, List<String> audience, boolean verifiedEmail) {
        return serviceFor(properties, audience, verifiedEmail, Instant.now().plusSeconds(60));
    }

    private GoogleIdentityService serviceFor(SecurityProperties properties, List<String> audience, boolean verifiedEmail,
                                             Instant expiresAt) {
        properties.getGoogle().setAudience("google-client");
        Jwt token = Jwt.withTokenValue("valid")
                .header("alg", "RS256")
                .issuer("https://accounts.google.com")
                .claim("aud", audience)
                .claim("email", "owner@example.test")
                .claim("email_verified", verifiedEmail)
                .expiresAt(expiresAt)
                .build();
        return new GoogleIdentityService(properties, ignored -> value -> token);
    }

    private SecurityProperties properties() {
        SecurityProperties properties = new SecurityProperties();
        SecurityProperties.EmailRoles owner = new SecurityProperties.EmailRoles();
        owner.setEmail("owner@example.test");
        owner.setRoles(List.of("ROLE_ADMIN", "ROLE_EDITOR"));
        SecurityProperties.EmailRoles empty = new SecurityProperties.EmailRoles();
        empty.setEmail("editor@example.test");
        properties.getGoogle().setAllowedRoles(List.of(owner, empty));
        return properties;
    }
}
