package org.open4goods.nudgerfrontapi.config;

import org.open4goods.commons.services.TextEmbeddingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Test-only configuration supplying a lightweight {@link TextEmbeddingService}
 * so integration tests do not depend on external DJL model downloads.
 */
@Configuration
public class TestTextEmbeddingConfig
{
    @Bean
    @Primary
    TextEmbeddingService testTextEmbeddingService()
    {
        return text -> new float[] { 0.0f, 0.0f };
    }
}
