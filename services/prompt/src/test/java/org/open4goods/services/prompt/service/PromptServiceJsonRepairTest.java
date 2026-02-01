package org.open4goods.services.prompt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.prompt.config.GenAiServiceType;
import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.service.provider.GenAiProvider;
import org.open4goods.services.prompt.service.provider.ProviderEvent;
import org.open4goods.services.prompt.service.provider.ProviderRegistry;
import org.open4goods.services.prompt.service.provider.ProviderRequest;
import org.open4goods.services.prompt.service.provider.ProviderResult;
import org.open4goods.services.serialisation.service.SerialisationService;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Flux;

class PromptServiceJsonRepairTest {

    private PromptService promptService;
    private PromptServiceConfig config;
    private EvaluationService evaluationService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        config = new PromptServiceConfig();
        evaluationService = org.mockito.Mockito.mock(EvaluationService.class);
        config.setEnabled(true);
        config.setCacheTemplates(true);
        config.setPromptsTemplatesFolder(tempDir.toString());
        config.setRepairEnabled(true);

        when(evaluationService.thymeleafEval(anyMap(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1, String.class));

        String yaml = """
                key: "repair-prompt"
                aiService: "OPEN_AI"
                systemPrompt: "System prompt"
                userPrompt: "User prompt"
                options:
                  model: "gpt-4o-mini"
                """;
        Files.writeString(tempDir.resolve("repair-prompt.yml"), yaml);

        SerialisationService serialisationService = new SerialisationService();
        GenAiProvider stubProvider = new JsonRepairStubProvider();
        ProviderRegistry registry = new ProviderRegistry(java.util.List.of(stubProvider));
        promptService = new PromptService(config, serialisationService, evaluationService, new SimpleMeterRegistry(), registry);
    }

    @Test
    void shouldRepairInvalidJsonResponses() throws Exception {
        PromptResponse<TestPayload> response = promptService.objectPrompt(
                "repair-prompt",
                Map.of(),
                TestPayload.class
        );

        assertThat(response.getBody().name()).isEqualTo("fixed");
    }

    private static final class JsonRepairStubProvider implements GenAiProvider {

        private final AtomicInteger calls = new AtomicInteger(0);

        @Override
        public GenAiServiceType service() {
            return GenAiServiceType.OPEN_AI;
        }

        @Override
        public ProviderResult generateText(ProviderRequest request) {
            if (calls.incrementAndGet() == 1) {
                return new ProviderResult(service(), "stub", "{invalid", "{invalid", Map.of());
            }
            return new ProviderResult(service(), "stub", "{\"name\":\"fixed\"}", "{\"name\":\"fixed\"}", Map.of());
        }

		@Override
		public Flux<ProviderEvent> generateTextStream(ProviderRequest request) {
			// TODO Auto-generated method stub
			return null;
		}
    }

    private record TestPayload(String name) {
    }
}
