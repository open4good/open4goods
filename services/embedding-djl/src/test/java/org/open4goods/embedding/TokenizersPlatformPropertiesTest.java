package org.open4goods.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import ai.djl.util.Platform;

/**
 * Verifies tokenizers engine fallback properties are available on the classpath.
 */
class TokenizersPlatformPropertiesTest
{
    @Test
    void shouldExposeFallbackNativeTokenizersProperties()
            throws Exception
    {
        try (InputStream stream = getClass().getResourceAsStream("/native/lib/tokenizers.properties"))
        {
            assertThat(stream).isNotNull();
            Properties properties = new Properties();
            properties.load(stream);
            assertThat(properties.getProperty("tokenizers_version")).isEqualTo("0.21.0-0.36.0");
            assertThat(properties.getProperty("djl_version")).isEqualTo("0.36.0");
        }
    }

    @Test
    void shouldExposeTokenizersVersionFromEngineProperties()
    {
        Platform platform = Platform.fromSystem("tokenizers");

        assertThat(platform.getVersion()).isEqualTo("0.21.0-0.36.0");
        assertThat(platform.getApiVersion()).isEqualTo("0.36.0");
    }
}
