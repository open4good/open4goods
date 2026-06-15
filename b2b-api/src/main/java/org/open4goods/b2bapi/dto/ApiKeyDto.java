package org.open4goods.b2bapi.dto;

import java.time.Instant;
import java.util.UUID;
import org.open4goods.b2bapi.model.ApiKeyStatus;

/**
 * API key metadata safe for repeated display.
 *
 * @param id API key id
 * @param name human-readable key name
 * @param keyPrefix non-secret key prefix
 * @param status lifecycle status
 * @param createdBy user id that created the key
 * @param createdAt creation instant
 * @param lastUsedAt last successful use, when known
 * @param revokedAt revocation instant, when revoked
 */
public record ApiKeyDto(
        UUID id,
        String name,
        String keyPrefix,
        ApiKeyStatus status,
        UUID createdBy,
        Instant createdAt,
        Instant lastUsedAt,
        Instant revokedAt) {
}
