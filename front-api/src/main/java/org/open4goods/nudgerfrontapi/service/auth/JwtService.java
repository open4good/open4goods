package org.open4goods.nudgerfrontapi.service.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;

import org.open4goods.nudgerfrontapi.config.SecurityProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Utility service to issue and validate JWT tokens for the frontend API.
 */
@Service
public class JwtService {

    private final SecurityProperties properties;

    public JwtService(SecurityProperties properties) {
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
        return Jwts.builder()
                .setSubject(auth.getName())
                .claim("roles", auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generate a refresh token for the given authentication.
     */
    public String generateRefreshToken(Authentication auth) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getRefreshTokenExpiry());
        return Jwts.builder()
                .setSubject(auth.getName())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate a token and return the authentication subject.
     */
    public String validateRefreshToken(String token) {
        return Jwts.parser()
                .setSigningKey(Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
