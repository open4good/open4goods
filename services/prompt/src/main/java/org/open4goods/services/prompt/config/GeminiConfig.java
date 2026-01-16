package org.open4goods.services.prompt.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class GeminiConfig {

    private static final String GOOGLE_GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai/";

    @Bean("geminiChatModel")
    @ConditionalOnProperty(prefix = "gen-ai-config", name = "gemini-api-key")
    public OpenAiChatModel geminiChatModel(PromptServiceConfig config) {
        String apiKey = config.getGeminiApiKey();

        if (!StringUtils.hasText(apiKey) || "mock".equalsIgnoreCase(apiKey)) {
             // In mock mode, we might want to return a mock or fail,
             // but here we let it try or it will interpret 'mock' as key.
             // Ideally we should handle mock properly, but let's stick to creating the beam.
        }

        // Create a dedicated OpenAiApi client pointing to Google's endpoint
        OpenAiApi googleGeminiApi = new OpenAiApi(GOOGLE_GEMINI_BASE_URL, apiKey);

        // Create the ChatModel with this specific API client
        return new OpenAiChatModel(googleGeminiApi, OpenAiChatOptions.builder()
                .model("gemini-2.0-flash") // Default model
                .build());
    }
}
