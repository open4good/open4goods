package org.open4goods.b2bapi.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies HS256 dashboard session tokens.
 */
@Service
public class JwtTokenService {

    private static final String ISSUER = "product-data-api";
    private static final String CLAIM_ORGANIZATION_ID = "org_id";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_PLATFORM_ADMIN = "platform_admin";
    private static final String CLAIM_TOKEN_USE = "token_use";

    private final B2bApiProperties properties;
    private final Clock clock;

    @Autowired
    public JwtTokenService(final B2bApiProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtTokenService(final B2bApiProperties properties, final Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Issues a fresh access and refresh token pair for the given user session.
     *
     * @param user authenticated dashboard user
     * @param organization active organization context
     * @return signed access and refresh JWTs
     */
    public JwtTokenPair issueTokenPair(final User user, final Organization organization) {
        final Instant issuedAt = clock.instant();
        final Instant accessExpiresAt = issuedAt.plus(properties.getSecurity().getAccessTokenTtl());
        final Instant refreshExpiresAt = issuedAt.plus(properties.getSecurity().getRefreshTokenTtl());

        return new JwtTokenPair(
                sign(user, organization, JwtTokenType.ACCESS, issuedAt, accessExpiresAt),
                accessExpiresAt,
                sign(user, organization, JwtTokenType.REFRESH, issuedAt, refreshExpiresAt),
                refreshExpiresAt);
    }

    /**
     * Verifies and parses a dashboard JWT.
     *
     * @param token signed JWT
     * @param expectedType expected token use
     * @return verified token claims
     */
    public JwtTokenClaims verify(final String token, final JwtTokenType expectedType) {
        try {
            final SignedJWT signedJwt = SignedJWT.parse(token);
            final boolean validSignature = signedJwt.verify(new MACVerifier(secretBytes()));
            if (!validSignature) {
                throw new IllegalArgumentException("Invalid JWT signature");
            }

            final JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
            final Instant expiresAt = claims.getExpirationTime().toInstant();
            if (!expiresAt.isAfter(clock.instant())) {
                throw new IllegalArgumentException("Expired JWT");
            }
            if (!ISSUER.equals(claims.getIssuer())) {
                throw new IllegalArgumentException("Invalid JWT issuer");
            }

            final JwtTokenType tokenType = JwtTokenType.valueOf(claims.getStringClaim(CLAIM_TOKEN_USE));
            if (tokenType != expectedType) {
                throw new IllegalArgumentException("Unexpected JWT token use");
            }

            return new JwtTokenClaims(
                    UUID.fromString(claims.getSubject()),
                    UUID.fromString(claims.getStringClaim(CLAIM_ORGANIZATION_ID)),
                    claims.getStringClaim(CLAIM_EMAIL),
                    Boolean.TRUE.equals(claims.getBooleanClaim(CLAIM_PLATFORM_ADMIN)),
                    tokenType,
                    expiresAt);
        } catch (final JOSEException | ParseException | RuntimeException exception) {
            throw new IllegalArgumentException("Invalid JWT", exception);
        }
    }

    private String sign(final User user, final Organization organization, final JwtTokenType tokenType,
            final Instant issuedAt, final Instant expiresAt) {
        try {
            final JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(ISSUER)
                    .subject(user.getId().toString())
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(Date.from(issuedAt))
                    .expirationTime(Date.from(expiresAt))
                    .claim(CLAIM_ORGANIZATION_ID, organization.getId().toString())
                    .claim(CLAIM_EMAIL, user.getEmail())
                    .claim(CLAIM_PLATFORM_ADMIN, user.isPlatformAdmin())
                    .claim(CLAIM_TOKEN_USE, tokenType.name())
                    .build();

            final SignedJWT signedJwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.HS256).type(com.nimbusds.jose.JOSEObjectType.JWT).build(),
                    claims);
            signedJwt.sign(new MACSigner(secretBytes()));
            return signedJwt.serialize();
        } catch (final JOSEException exception) {
            throw new IllegalStateException("Unable to sign JWT", exception);
        }
    }

    private byte[] secretBytes() {
        final byte[] secret = properties.getSecurity().getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("b2b.security.jwt-secret must be at least 32 bytes for HS256");
        }
        return secret;
    }
}
