package org.open4goods.embedding.config;

import org.open4goods.embedding.health.DjlEmbeddingHealthIndicator;
import org.open4goods.embedding.service.DefaultTextModelFactory;
import org.open4goods.embedding.service.DjlTextEmbeddingService;
import org.open4goods.embedding.service.EmbeddingModelFactory;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that exposes a {@link DjlTextEmbeddingService} bean when the module is on the classpath.
 */
@AutoConfiguration
@EnableConfigurationProperties(DjlEmbeddingProperties.class)
@ConditionalOnProperty(prefix = "embedding", name = "enabled", matchIfMissing = true)
public class DjlEmbeddingAutoConfiguration
{
    @Bean
    @ConditionalOnMissingBean(EmbeddingModelFactory.class)
    EmbeddingModelFactory embeddingModelFactory()
    {
        return new DefaultTextModelFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    DjlTextEmbeddingService djlTextEmbeddingService(DjlEmbeddingProperties properties, EmbeddingModelFactory modelFactory)
    {
        return new DjlTextEmbeddingService(properties, modelFactory);
    }

    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnBean(DjlTextEmbeddingService.class)
    DjlEmbeddingHealthIndicator djlEmbeddingHealthIndicator(DjlTextEmbeddingService service)
    {
        return new DjlEmbeddingHealthIndicator(service);
    }
}
