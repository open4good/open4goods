package org.open4goods.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ai.djl.util.Platform;

/**
 * Verifies tokenizers engine fallback properties are available on the classpath.
 */
class TokenizersPlatformPropertiesTest
{
    @Test
    void shouldExposeTokenizersVersionFromEngineProperties()
    {
        Platform platform = Platform.fromSystem("tokenizers");

        assertThat(platform.getVersion()).isEqualTo("0.21.0-0.36.0");
        assertThat(platform.getApiVersion()).isEqualTo("0.36.0");
    }
}
