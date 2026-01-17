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
    public OpenAiApi openAiCustomApi(PromptServiceConfig config,
                                     org.springframework.web.client.RestClient.Builder restClientBuilder,
                                     org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder) {
        String apiKey = config.getOpenaiApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("OpenAI API key is missing in gen-ai-config");
        }
        return new OpenAiApi(
                "https://api.openai.com",
                new org.springframework.ai.model.ApiKey() {
                    @Override
                    public String getValue() {
                        return apiKey;
                    }
                },
                org.springframework.util.CollectionUtils.toMultiValueMap(java.util.Map.of()),
                "/v1/chat/completions",
                "/v1/embeddings",
                restClientBuilder,
                webClientBuilder,
                org.springframework.ai.retry.RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER
        );
    }
}
