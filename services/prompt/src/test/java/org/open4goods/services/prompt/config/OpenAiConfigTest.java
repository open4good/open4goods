package org.open4goods.services.prompt.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.services.prompt.service.provider.OpenAiProvider;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootTest(classes = {OpenAiConfig.class, OpenAiProvider.class, OpenAiConfigTest.TestConfig.class})
@EnableConfigurationProperties(PromptServiceConfig.class)
@TestPropertySource(properties = {
    "gen-ai-config.openai-api-key=test-key"
})
class OpenAiConfigTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    @Qualifier("openAiCustomApi")
    private OpenAiApi openAiApi;

    @Autowired
    private OpenAiProvider openAiProvider;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
        assertThat(openAiApi).isNotNull();
        assertThat(openAiProvider).isNotNull();
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @Bean
        public org.springframework.web.client.RestClient.Builder restClientBuilder() {
            return org.springframework.web.client.RestClient.builder();
        }

        @Bean
        public org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder() {
            return org.springframework.web.reactive.function.client.WebClient.builder();
        }

        @Bean
        public org.springframework.ai.model.tool.ToolCallingManager toolCallingManager() {
            return org.springframework.ai.model.tool.ToolCallingManager.builder().build();
        }

        @Bean
        public org.springframework.retry.support.RetryTemplate retryTemplate() {
            return new org.springframework.retry.support.RetryTemplate();
        }

        @Bean
        public io.micrometer.observation.ObservationRegistry observationRegistry() {
            return io.micrometer.observation.ObservationRegistry.create();
        }
    }
}
