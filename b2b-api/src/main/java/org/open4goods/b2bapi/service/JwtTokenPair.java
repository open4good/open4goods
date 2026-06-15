package org.open4goods.b2bapi.service;

import java.time.Instant;

/**
 * Pair of dashboard session JWTs and their expiration instants.
 */
public record JwtTokenPair(
        String accessToken,
        Instant accessExpiresAt,
        String refreshToken,
        Instant refreshExpiresAt) {
}
