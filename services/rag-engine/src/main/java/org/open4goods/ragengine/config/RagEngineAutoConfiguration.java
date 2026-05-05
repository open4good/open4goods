package org.open4goods.ragengine.config;

import org.open4goods.ragengine.service.RagProviderRouter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot auto-configuration entry point for the RAG engine module.
 */
@Configuration
@EnableConfigurationProperties(RagEngineProperties.class)
public class RagEngineAutoConfiguration
{
    /**
     * Creates the default provider router responsible for use-case based provider selection.
     *
     * @param properties validated module configuration
     * @return default router implementation
     */
    @Bean
    @ConditionalOnMissingBean
    public RagProviderRouter ragProviderRouter(final RagEngineProperties properties)
    {
        return new RagProviderRouter(properties);
    }
}
