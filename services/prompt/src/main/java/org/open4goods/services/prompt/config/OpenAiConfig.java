package org.open4goods.services.prompt.config;

import org.springframework.ai.openai.api.OpenAiApi;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class OpenAiConfig {

    @Bean("openAiCustomApi")
    @ConditionalOnProperty(prefix = "gen-ai-config", name = "openai-api-key")
    public OpenAiApi openAiCustomApi(PromptServiceConfig config) {
        String apiKey = config.getOpenaiApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("OpenAI API key is missing in gen-ai-config");
        }
        return new OpenAiApi(apiKey);
    }
}
