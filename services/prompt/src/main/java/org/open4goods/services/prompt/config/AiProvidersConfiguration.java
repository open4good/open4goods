package org.open4goods.services.prompt.config;

import org.open4goods.services.prompt.service.provider.GeminiProvider;
import org.open4goods.services.prompt.service.provider.OpenAiProvider;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Provider configuration using Spring AI auto-configured model beans.
 */
@Configuration
public class AiProvidersConfiguration {

    @Bean
    @ConditionalOnBean(OpenAiChatModel.class)
    public OpenAiProvider openAiProvider(OpenAiChatModel chatModel, Environment environment) {
        return new OpenAiProvider(chatModel, environment);
    }

    @Bean
    @ConditionalOnBean(GoogleGenAiChatModel.class)
    public GeminiProvider geminiProvider(GoogleGenAiChatModel chatModel) {
        return new GeminiProvider(chatModel);
    }
}
