package org.open4goods.services.prompt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.prompt.config.PromptConfig;
import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.open4goods.services.prompt.config.RetrievalMode;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.service.provider.GenAiProvider;
import org.open4goods.services.prompt.service.provider.ProviderRegistry;
import org.open4goods.services.prompt.service.provider.ProviderResult;
import org.open4goods.services.prompt.config.GenAiServiceType;
import org.open4goods.services.serialisation.service.SerialisationService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@ExtendWith(MockitoExtension.class)
class PromptServicePostProcessTest {

    @Mock
    private PromptServiceConfig genAiConfig;
    @Mock
    private SerialisationService serialisationService;
    @Mock
    private EvaluationService evaluationService;
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private ProviderRegistry providerRegistry;
    @Mock
    private GenAiProvider provider;

    private PromptService promptService;

    @BeforeEach
    void setUp() {

        when(meterRegistry.counter(any(String.class), any(String[].class))).thenReturn(mock(Counter.class));
        
        promptService = new PromptService(genAiConfig, serialisationService, evaluationService, meterRegistry, providerRegistry) {
            @Override
            public void loadPrompts(String folderPath) {
                // Do nothing to avoid file loading
            }
            
            @Override
            public PromptConfig getPromptConfig(String promptKey) {
                PromptConfig config = new PromptConfig();
                config.setAiService(GenAiServiceType.OPEN_AI);
                config.setRetrievalMode(RetrievalMode.EXTERNAL_SOURCES);
                config.setSystemPrompt("Sys");
                config.setUserPrompt("User");
                return config;
            }
        };
    }

    @Test
    void testPostProcessReplacements() throws Exception {
        // Configure replacements
        Map<String, String> replacements = new HashMap<>();
        replacements.put("—", "-");
        replacements.put("…", "...");
        replacements.put("foo", "bar");
        when(genAiConfig.getReplacements()).thenReturn(replacements);

        // Mock provider response
        String rawContent = "This is a test — with some … foo characters.";
        ProviderResult result = new ProviderResult(GenAiServiceType.OPEN_AI, "model", "raw", rawContent, new HashMap<>());
        when(providerRegistry.getProvider(any())).thenReturn(provider);
        when(provider.generateText(any())).thenReturn(result);
        
        // Mock evaluation service
        when(evaluationService.thymeleafEval(any(Map.class), anyString())).thenReturn("evaluated");
        when(serialisationService.toYamLiteral(any())).thenReturn("yaml");
        when(serialisationService.fromYaml(anyString(), eq(PromptConfig.class))).thenReturn(new PromptConfig());

        // Execute prompt
        PromptResponse<String> response = promptService.prompt("test", new HashMap<>());

        // Verify result
        String expectedContent = "This is a test - with some ... bar characters.";
        assertThat(response.getBody()).isEqualTo(expectedContent);
    }
    
    @Test
    void testPostProcessNoReplacementsConfigured() throws Exception {
        // Configure no replacements
        when(genAiConfig.getReplacements()).thenReturn(null);

        // Mock provider response
        String rawContent = "This is a test — with some … foo characters.";
        ProviderResult result = new ProviderResult(GenAiServiceType.OPEN_AI, "model", "raw", rawContent, new HashMap<>());
        when(providerRegistry.getProvider(any())).thenReturn(provider);
        when(provider.generateText(any())).thenReturn(result);
        
        // Mock evaluation service
        when(evaluationService.thymeleafEval(any(Map.class), anyString())).thenReturn("evaluated");
        when(serialisationService.toYamLiteral(any())).thenReturn("yaml");
        when(serialisationService.fromYaml(anyString(), eq(PromptConfig.class))).thenReturn(new PromptConfig());

        // Execute prompt
        PromptResponse<String> response = promptService.prompt("test", new HashMap<>());

        // Verify result is unchanged
        assertThat(response.getBody()).isEqualTo(rawContent);
    }
}
