package org.open4goods.embedding.config;

import org.open4goods.embedding.service.image.AbstractImageModelFactory;
import org.open4goods.embedding.service.image.DefaultImageModelFactory;
import org.open4goods.embedding.service.image.DjlImageEmbeddingService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for local DJL image embeddings.
 */
@AutoConfiguration
@EnableConfigurationProperties(DjlEmbeddingProperties.class)
@ConditionalOnProperty(prefix = "embedding", name = "enabled", matchIfMissing = true)
public class DjlEmbeddingAutoConfiguration
{
    @Bean
    @ConditionalOnMissingBean(AbstractImageModelFactory.class)
    AbstractImageModelFactory imageModelFactory()
    {
        return new DefaultImageModelFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    DjlImageEmbeddingService djlImageEmbeddingService(DjlEmbeddingProperties properties, AbstractImageModelFactory modelFactory)
    {
        return new DjlImageEmbeddingService(properties, modelFactory);
    }
}
