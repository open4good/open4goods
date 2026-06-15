package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for opaque API key generation and hashing.
 */
class ApiKeySecretGeneratorTest {

    private final ApiKeySecretGenerator generator = new ApiKeySecretGenerator();

    @Test
    void generatesPdapiKeyPrefixAndSha256Hash() {
        final ApiKeySecret secret = generator.generate();

        assertThat(secret.clearKey()).startsWith("pdapi_");
        assertThat(secret.clearKey().length()).isGreaterThanOrEqualTo(40);
        assertThat(secret.keyPrefix()).startsWith("pdapi_");
        assertThat(secret.keyPrefix()).hasSize("pdapi_".length() + 8);
        assertThat(secret.keyHash()).hasSize(64);
        assertThat(secret.keyHash()).isEqualTo(generator.sha256Hex(secret.clearKey()));
    }
}
