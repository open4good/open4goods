package org.open4goods.b2bapi.dto;

/**
 * API key response that includes the clear secret exactly once.
 *
 * @param key metadata
 * @param clearKey clear API key, returned only from create and rotate operations
 */
public record ApiKeySecretResponse(
        ApiKeyDto key,
        String clearKey) {
}
