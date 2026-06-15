package org.open4goods.b2bapi.service;

import java.time.Instant;
import java.util.UUID;

/**
 * Verified dashboard session token claims.
 */
public record JwtTokenClaims(
        UUID userId,
        UUID organizationId,
        String email,
        boolean platformAdmin,
        JwtTokenType tokenType,
        Instant expiresAt) {
}
