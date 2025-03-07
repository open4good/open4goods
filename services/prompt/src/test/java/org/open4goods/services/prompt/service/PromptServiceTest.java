package org.open4goods.services.prompt.service;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.open4goods.services.prompt.config.PromptConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Minimal test configuration to bootstrap the Spring context.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = {"org.open4goods.services.prompt"})

class PromptServiceTest {

    private PromptService genAiService;
    private PromptServiceConfig mockConfig;
    private OpenAiApi openAiApi;
    private OpenAiApi perplexityApi;
    private SerialisationService serialisationService;
    private EvaluationService evaluationService;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        // Create mocks for dependencies
        mockConfig = mock(PromptServiceConfig.class);
        openAiApi = mock(OpenAiApi.class);
        perplexityApi = mock(OpenAiApi.class);
        serialisationService = mock(SerialisationService.class);
        evaluationService = mock(EvaluationService.class);
        meterRegistry = mock(MeterRegistry.class);

        // Stub configuration methods
        when(mockConfig.isEnabled()).thenReturn(true);
        when(mockConfig.isCacheTemplates()).thenReturn(true);
        when(mockConfig.getPromptsTemplatesFolder()).thenReturn("src/test/resources/prompts");

        // Stub serialization behavior (for simplicity, assume identity conversion)
        try {
        	when(serialisationService.toJson(any())).thenAnswer(invocation -> invocation.getArgument(0).toString());
			when(serialisationService.fromJson(anyString(), eq(PromptConfig.class)))
			        .thenReturn(new PromptConfig());
		} catch (Exception e) {
			fail(e);
		}

        // Instantiate the service under test
        genAiService = new PromptService(mockConfig, perplexityApi, openAiApi, serialisationService, evaluationService, meterRegistry);
    }

    @Test
    void testPromptNotFound() {
        // Expect a ResourceNotFoundException when a non-existent prompt key is used
        Map<String, Object> variables = new HashMap<>();
        assertThrows(ResourceNotFoundException.class, () -> genAiService.prompt("nonExistentKey", variables));
    }

    // Additional tests should be implemented to cover jsonPrompt and other methods.
}
