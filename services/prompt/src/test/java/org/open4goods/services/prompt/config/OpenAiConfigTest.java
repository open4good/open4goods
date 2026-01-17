package org.open4goods.services.prompt.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.services.prompt.service.provider.OpenAiProvider;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootTest(classes = {OpenAiConfig.class, OpenAiProvider.class})
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
}
