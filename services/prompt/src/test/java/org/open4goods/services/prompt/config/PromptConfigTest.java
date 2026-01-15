package org.open4goods.services.prompt.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.services.serialisation.service.SerialisationService;

class PromptConfigTest {

    @Test
    void shouldParsePromptOptionsWithProviderOverrides() throws Exception {
        SerialisationService serialisationService = new SerialisationService();
        String yaml = """
                key: "test-prompt"
                aiService: "OPEN_AI"
                retrievalMode: "MODEL_WEB_SEARCH"
                systemPrompt: "system"
                userPrompt: "user"
                options:
                  model: "gpt-4o"
                  temperature: 0.2
                  max_tokens: 128
                  custom_option: "value"
                """;

        PromptConfig config = serialisationService.fromYaml(yaml, PromptConfig.class);

        assertThat(config.getRetrievalMode()).isEqualTo(RetrievalMode.MODEL_WEB_SEARCH);
        assertThat(config.getOptions().getModel()).isEqualTo("gpt-4o");
        assertThat(config.getOptions().getTemperature()).isEqualTo(0.2);
        assertThat(config.getOptions().getMaxTokens()).isEqualTo(128);
        assertThat(config.getProviderOptions()).isEqualTo(Map.of("custom_option", "value"));
    }

    @Test
    void shouldDefaultToExternalSourcesWhenMissing() throws Exception {
        SerialisationService serialisationService = new SerialisationService();
        String yaml = """
                key: "test-prompt"
                aiService: "OPEN_AI"
                systemPrompt: "system"
                userPrompt: "user"
                """;

        PromptConfig config = serialisationService.fromYaml(yaml, PromptConfig.class);

        assertThat(config.getRetrievalMode()).isEqualTo(RetrievalMode.EXTERNAL_SOURCES);
    }
}
