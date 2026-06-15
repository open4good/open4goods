package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.model.OidcProvider;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.User;

/**
 * Unit tests for dashboard JWT signing and verification.
 */
class JwtTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z");

    @Test
    void issuesAndVerifiesAccessAndRefreshTokens() {
        final B2bApiProperties properties = properties();
        final JwtTokenService service = new JwtTokenService(properties, Clock.fixed(NOW, ZoneOffset.UTC));
        final User user = user();
        final Organization organization = organization();

        final JwtTokenPair tokens = service.issueTokenPair(user, organization);

        final JwtTokenClaims accessClaims = service.verify(tokens.accessToken(), JwtTokenType.ACCESS);
        final JwtTokenClaims refreshClaims = service.verify(tokens.refreshToken(), JwtTokenType.REFRESH);

        assertThat(accessClaims.userId()).isEqualTo(user.getId());
        assertThat(accessClaims.organizationId()).isEqualTo(organization.getId());
        assertThat(accessClaims.email()).isEqualTo("owner@example.com");
        assertThat(accessClaims.platformAdmin()).isTrue();
        assertThat(accessClaims.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(refreshClaims.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
    }

    @Test
    void rejectsUnexpectedTokenUse() {
        final JwtTokenService service = new JwtTokenService(properties(), Clock.fixed(NOW, ZoneOffset.UTC));
        final JwtTokenPair tokens = service.issueTokenPair(user(), organization());

        assertThatThrownBy(() -> service.verify(tokens.refreshToken(), JwtTokenType.ACCESS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid JWT");
    }

    @Test
    void rejectsWeakSecret() {
        final B2bApiProperties properties = properties();
        properties.getSecurity().setJwtSecret("too-short");
        final JwtTokenService service = new JwtTokenService(properties, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.issueTokenPair(user(), organization()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    private static B2bApiProperties properties() {
        final B2bApiProperties properties = new B2bApiProperties();
        properties.getSecurity().setJwtSecret("test-secret-with-at-least-32-bytes");
        return properties;
    }

    private static User user() {
        final User user = new User("owner@example.com", OidcProvider.GOOGLE, "google-subject");
        user.setPlatformAdmin(true);
        return user;
    }

    private static Organization organization() {
        return new Organization("Acme", "acme");
    }
}
