package org.open4goods.services.prompt.config;

import org.open4goods.services.prompt.service.provider.OpenAiProvider;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Provider configuration using Spring AI auto-configured model beans.
 * <p>
 * Declared as an AutoConfiguration so that {@code @AutoConfigureAfter} is honoured:
 * {@link OpenAiChatAutoConfiguration} must run first, otherwise
 * {@link OpenAiChatModel} is not yet registered and the conditional bean is skipped.
 * </p>
 */
@AutoConfiguration
@AutoConfigureAfter(OpenAiChatAutoConfiguration.class)
public class AiProvidersConfiguration {

    @Bean
    @ConditionalOnBean(OpenAiChatModel.class)
    public OpenAiProvider openAiProvider(OpenAiChatModel chatModel, Environment environment) {
        return new OpenAiProvider(chatModel, environment);
    }

}
