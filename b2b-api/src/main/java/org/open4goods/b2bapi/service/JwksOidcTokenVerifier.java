package org.open4goods.b2bapi.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.exception.InvalidCredentialsException;
import org.open4goods.b2bapi.model.OidcProvider;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;

/**
 * Shared JWKS-backed verifier for standard OIDC ID tokens.
 */
public abstract class JwksOidcTokenVerifier implements OidcTokenVerifier {

    private final OidcProvider provider;
    private final B2bApiProperties.OidcProvider properties;
    private final Clock clock;
    private final Map<String, NimbusJwtDecoder> decoders = new ConcurrentHashMap<>();

    protected JwksOidcTokenVerifier(
            final OidcProvider provider,
            final B2bApiProperties.OidcProvider properties,
            final Clock clock) {
        this.provider = provider;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public OidcProvider provider() {
        return provider;
    }

    @Override
    public OidcUserProfile verify(final String credential) {
        if (!StringUtils.hasText(properties.getClientId())) {
            throw new InvalidCredentialsException(provider + " OIDC client id is not configured");
        }
        try {
            final Jwt jwt = decoders.computeIfAbsent(properties.getJwksUri(),
                    jwksUri -> NimbusJwtDecoder.withJwkSetUri(jwksUri).build())
                    .decode(credential);
            validate(jwt);
            return new OidcUserProfile(
                    provider,
                    jwt.getSubject(),
                    jwt.getClaimAsString("email"),
                    Boolean.TRUE.equals(jwt.getClaim("email_verified")),
                    firstPresent(jwt.getClaimAsString("name"), jwt.getClaimAsString("preferred_username")),
                    jwt.getClaimAsString("picture"));
        } catch (final JwtException | IllegalArgumentException exception) {
            throw new InvalidCredentialsException(provider + " OIDC token verification failed", exception);
        }
    }

    protected void validate(final Jwt jwt) {
        if (!StringUtils.hasText(jwt.getSubject())) {
            throw new InvalidCredentialsException(provider + " OIDC token is missing subject");
        }
        if (jwt.getIssuer() == null || !issuerMatches(jwt.getIssuer().toString())) {
            throw new InvalidCredentialsException(provider + " OIDC token issuer mismatch");
        }
        final List<String> audience = jwt.getAudience();
        if (audience == null || !audience.contains(properties.getClientId())) {
            throw new InvalidCredentialsException(provider + " OIDC token audience mismatch");
        }
        final Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null || !expiresAt.isAfter(clock.instant())) {
            throw new InvalidCredentialsException(provider + " OIDC token expired");
        }
    }

    protected boolean issuerMatches(final String tokenIssuer) {
        return properties.getIssuer().equals(tokenIssuer);
    }

    protected B2bApiProperties.OidcProvider oidcProperties() {
        return properties;
    }

    private static String firstPresent(final String first, final String second) {
        return StringUtils.hasText(first) ? first : second;
    }
}
