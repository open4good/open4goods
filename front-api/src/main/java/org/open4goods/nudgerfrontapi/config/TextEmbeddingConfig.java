package org.open4goods.nudgerfrontapi.config;

import org.open4goods.commons.services.TextEmbeddingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TextEmbeddingConfig
{

	@Bean
    @ConditionalOnMissingBean(TextEmbeddingService.class)
    TextEmbeddingService textEmbeddingService()
    {
		System.err.println("No embedding service found");
    	return text -> null;
    }
}
