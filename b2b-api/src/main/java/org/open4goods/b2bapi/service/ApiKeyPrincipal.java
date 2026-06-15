package org.open4goods.b2bapi.service;

import java.util.UUID;

/**
 * Authenticated machine principal for external Product Data API calls.
 *
 * @param organizationId owning organization id
 * @param apiKeyId API key id
 */
public record ApiKeyPrincipal(
        UUID organizationId,
        UUID apiKeyId) {
}
