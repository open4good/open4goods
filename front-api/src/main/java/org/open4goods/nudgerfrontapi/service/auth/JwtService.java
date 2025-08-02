package org.open4goods.nudgerfrontapi.service.auth;

import java.time.Instant;

import org.open4goods.nudgerfrontapi.config.SecurityProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

/**
 * Utility service to issue and validate JWT tokens for the frontend API.
 */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final SecurityProperties properties;

    public JwtService(JwtEncoder encoder, JwtDecoder decoder, SecurityProperties properties) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.properties = properties;
    }

    public SecurityProperties getProperties() {
        return properties;
    }

    /**
     * Generate an access token for the given authentication.
     */
    public String generateAccessToken(Authentication auth) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getAccessTokenExpiry());
        JwsHeader header = JwsHeader.with(() -> "HS256").build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(auth.getName())
                .claim("roles", auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).toList())
                .issuedAt(now)
                .expiresAt(exp)
                .build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Generate a refresh token for the given authentication.
     */
    public String generateRefreshToken(Authentication auth) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getRefreshTokenExpiry());
        JwsHeader header = JwsHeader.with(() -> "HS256").build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(auth.getName())
                .issuedAt(now)
                .expiresAt(exp)
                .build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Validate a token and return the authentication subject.
     */
    public String validateRefreshToken(String token) {
        return decoder.decode(token).getSubject();
    }
}
