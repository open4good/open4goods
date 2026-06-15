package org.open4goods.b2bapi.service;

/**
 * Generated clear API key and derived storage values.
 *
 * @param clearKey clear key returned once to the caller
 * @param keyPrefix non-secret display prefix
 * @param keyHash SHA-256 hex hash of the clear key
 */
public record ApiKeySecret(
        String clearKey,
        String keyPrefix,
        String keyHash) {
}
