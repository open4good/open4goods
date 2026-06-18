package org.open4goods.services.prompt.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.open4goods.services.prompt.service.provider.GeminiProvider;
import org.open4goods.services.prompt.service.provider.OpenAiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.util.StringUtils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.genai.Client;

import io.micrometer.observation.ObservationRegistry;

/**
 * Provider configuration using Spring AI auto-configured model beans.
 * <p>
 * Declared as an AutoConfiguration so that {@code @AutoConfigureAfter} is honoured:
 * {@link OpenAiChatAutoConfiguration} must run first, otherwise
 * {@link OpenAiChatModel} is not yet registered and the conditional bean is skipped.
 * </p>
 * <p>
 * The Spring AI single-model selector ({@code spring.ai.model.chat}) defaults to
 * {@code openai}, which disables the Google GenAI chat auto-configuration. Because the
 * application needs BOTH OpenAI (attribute extraction) and Gemini (review text) providers
 * at the same time, the Gemini {@link GoogleGenAiChatModel} is created here explicitly,
 * in Vertex AI mode, from the service-account JSON already configured under
 * {@code gen-ai-config.google-api-json}. The bean only registers when that credential is
 * present and no other {@link GoogleGenAiChatModel} bean exists.
 * </p>
 */
@AutoConfiguration
@AutoConfigureAfter(OpenAiChatAutoConfiguration.class)
public class AiProvidersConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AiProvidersConfiguration.class);

    @Bean
    @ConditionalOnBean(OpenAiChatModel.class)
    public OpenAiProvider openAiProvider(OpenAiChatModel chatModel, Environment environment,
            PromptServiceConfig promptServiceConfig) {
        return new OpenAiProvider(chatModel, environment, promptServiceConfig);
    }

    /**
     * Builds the Vertex AI Gemini chat model from the configured service-account JSON.
     * <p>
     * Mirrors {@code GoogleGenAiChatAutoConfiguration} (which is disabled here because
     * {@code spring.ai.model.chat=openai}). Project id and location fall back to the
     * Vertex batch configuration so a single credential set drives both realtime and
     * batch Gemini calls.
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(GoogleGenAiChatModel.class)
    @ConditionalOnProperty(prefix = "gen-ai-config", name = "google-api-json")
    public GoogleGenAiChatModel googleGenAiChatModel(PromptServiceConfig promptServiceConfig, Environment environment,
            ObjectProvider<ToolCallingManager> toolCallingManager,
            ObjectProvider<RetryTemplate> retryTemplate,
            ObjectProvider<ObservationRegistry> observationRegistry) throws IOException {

        String projectId = firstNonBlank(environment.getProperty("spring.ai.google.genai.project-id"),
                environment.getProperty("vertex.batch.project-id"));
        String location = firstNonBlank(environment.getProperty("spring.ai.google.genai.location"),
                environment.getProperty("vertex.batch.location"), "us-central1");
        String model = firstNonBlank(environment.getProperty("spring.ai.google.genai.chat.options.model"),
                "gemini-2.5-pro");

        if (!StringUtils.hasText(projectId)) {
            throw new IllegalStateException("Cannot build Gemini chat model: no Vertex project id configured "
                    + "(spring.ai.google.genai.project-id or vertex.batch.project-id).");
        }

        GoogleCredentials credentials;
        try (ByteArrayInputStream is = new ByteArrayInputStream(
                promptServiceConfig.getGoogleApiJson().getBytes(StandardCharsets.UTF_8))) {
            credentials = GoogleCredentials.fromStream(is);
        }

        Client client = Client.builder()
                .project(projectId)
                .location(location)
                .vertexAI(true)
                .credentials(credentials)
                .build();

        logger.info("Registering Vertex AI Gemini chat model (project={}, location={}, defaultModel={}).",
                projectId, location, model);

        return new GoogleGenAiChatModel(client,
                GoogleGenAiChatOptions.builder().model(model).build(),
                toolCallingManager.getIfUnique(() -> ToolCallingManager.builder().build()),
                retryTemplate.getIfUnique(() -> RetryUtils.DEFAULT_RETRY_TEMPLATE),
                observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP));
    }

    /**
     * Registers the Gemini provider as an explicit bean once the {@link GoogleGenAiChatModel}
     * exists. Declaring it here (rather than via {@code @Component} + class-level
     * {@code @ConditionalOnBean}) guarantees the condition is evaluated after the chat-model
     * bean definition, so the provider is reliably picked up by the {@code ProviderRegistry}.
     */
    @Bean
    @ConditionalOnBean(GoogleGenAiChatModel.class)
    public GeminiProvider geminiProvider(GoogleGenAiChatModel googleGenAiChatModel) {
        return new GeminiProvider(googleGenAiChatModel);
    }

    private static String firstNonBlank(String... values) {
        if (values != null) {
            for (String value : values) {
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
        }
        return null;
    }

}
