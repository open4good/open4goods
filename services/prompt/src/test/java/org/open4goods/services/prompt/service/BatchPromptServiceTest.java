package org.open4goods.services.prompt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.prompt.config.GenAiServiceType;
import org.open4goods.services.prompt.config.PromptConfig;
import org.open4goods.services.prompt.config.PromptOptions;
import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.open4goods.services.prompt.config.RetrievalMode;
import org.open4goods.services.prompt.config.VertexBatchConfig;
import org.open4goods.services.prompt.model.BatchJob;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class BatchPromptServiceTest {

    private BatchPromptService batchPromptService;

    @Mock private PromptServiceConfig config;
    @Mock private VertexBatchConfig vertexBatchConfig;
    @Mock private EvaluationService evaluationService;
    @Mock private OpenAiBatchClient openAiBatchClient;
    @Mock private VertexGeminiBatchClient vertexGeminiBatchClient;
    @Mock private PromptService promptService;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @BeforeEach
    void setUp() {
        when(config.getBatchFolder()).thenReturn(System.getProperty("java.io.tmpdir") + "/batch-test");
        org.mockito.Mockito.lenient().when(config.getBatchMaxTokens()).thenReturn(100000);
        
        batchPromptService = new BatchPromptService(config, vertexBatchConfig, evaluationService, openAiBatchClient, vertexGeminiBatchClient, promptService);
    }

    @Test
    void batchPromptRequest_WithGeminiAndSearch_ShouldIncludeTools() throws IOException {
        // Setup
        String promptKey = "test-key";
        PromptConfig promptConfig = new PromptConfig();
        promptConfig.setAiService(GenAiServiceType.GEMINI);
        promptConfig.setRetrievalMode(RetrievalMode.MODEL_WEB_SEARCH);
        promptConfig.setUserPrompt("User prompt");
        promptConfig.setSystemPrompt("System prompt");
        promptConfig.setOptions(new PromptOptions());
        promptConfig.getOptions().setModel("gemini-1.5-pro");

        when(promptService.getPromptConfig(promptKey)).thenReturn(promptConfig);
        when(evaluationService.thymeleafEval(any(Map.class), anyString())).thenReturn("Evaluated Prompt");
        
        // Mock Vertex config validation
        when(vertexBatchConfig.getProjectId()).thenReturn("proj");
        when(vertexBatchConfig.getBucket()).thenReturn("bucket");
        when(vertexBatchConfig.getCredentialsJson()).thenReturn("{}");
        
        // Mock Gemini client to avoid actual upload/submit
        when(vertexGeminiBatchClient.uploadInputFile(anyString(), anyString())).thenReturn("gs://bucket/file");
        when(vertexGeminiBatchClient.submitBatchJob(anyString(), anyString(), anyString())).thenReturn("remote-job-id");

        // Execute
        List<Map<String, Object>> variables = List.of(Map.of("var", "val"));
        List<String> customIds = List.of("123");
        String jobId = batchPromptService.batchPromptRequest(promptKey, variables, customIds, String.class);

        // Verify that the generated submission file contains "tools" and "google_search"
        File batchFolder = new File(System.getProperty("java.io.tmpdir") + "/batch-test");
        File submissionFile = new File(batchFolder, "batch-" + jobId + "-submission.jsonl");
        
        assertThat(submissionFile).exists();
        
        List<String> lines = Files.readAllLines(submissionFile.toPath());
        assertThat(lines).hasSize(1);
        
        JsonNode jsonNode = objectMapper.readTree(lines.get(0));
        assertThat(jsonNode.has("request")).isTrue();
        JsonNode request = jsonNode.get("request");
        
        assertThat(request.has("tools")).isTrue();
        JsonNode tools = request.get("tools");
        assertThat(tools.isArray()).isTrue();
        assertThat(tools.get(0).has("google_search")).isTrue();
        JsonNode googleSearch = tools.get(0).get("google_search");
        assertThat(googleSearch.has("exclude_domains")).isTrue();
        assertThat(googleSearch.get("exclude_domains").isArray()).isTrue();
    }
    
    @Test
    void batchPromptRequest_WithGeminiAndNoSearch_ShouldNotIncludeTools() throws IOException {
        // Setup
        String promptKey = "test-key-no-search";
        PromptConfig promptConfig = new PromptConfig();
        promptConfig.setAiService(GenAiServiceType.GEMINI);
        promptConfig.setRetrievalMode(RetrievalMode.EXTERNAL_SOURCES); // Not MODEL_WEB_SEARCH
        promptConfig.setUserPrompt("User prompt");
        
        when(promptService.getPromptConfig(promptKey)).thenReturn(promptConfig);
        when(evaluationService.thymeleafEval(any(Map.class), anyString())).thenReturn("Evaluated Prompt");

        // Mock Vertex config validation
        when(vertexBatchConfig.getProjectId()).thenReturn("proj");
        when(vertexBatchConfig.getBucket()).thenReturn("bucket");
        when(vertexBatchConfig.getCredentialsJson()).thenReturn("{}");
        
        when(vertexGeminiBatchClient.uploadInputFile(anyString(), anyString())).thenReturn("gs://bucket/file");
        when(vertexGeminiBatchClient.submitBatchJob(anyString(), anyString(), anyString())).thenReturn("remote-job-id");

        // Execute
        List<Map<String, Object>> variables = List.of(Map.of("var", "val"));
        List<String> customIds = List.of("123");
        String jobId = batchPromptService.batchPromptRequest(promptKey, variables, customIds, String.class);

        // Verify
        File batchFolder = new File(System.getProperty("java.io.tmpdir") + "/batch-test");
        File submissionFile = new File(batchFolder, "batch-" + jobId + "-submission.jsonl");
        
        List<String> lines = Files.readAllLines(submissionFile.toPath());
        JsonNode jsonNode = objectMapper.readTree(lines.get(0));
        
        assertThat(jsonNode.has("request")).isTrue();
        JsonNode request = jsonNode.get("request");
        assertThat(request.has("tools")).isFalse();
    }
    
    @Test
    void checkStatus_WithBatchPrefix_ShouldStripPrefix() {
        // Setup
        String jobId = "12345-abcde";
        BatchJob job = new BatchJob();
        job.setId(jobId);
        job.setProvider(GenAiServiceType.OPEN_AI);
        job.setRemoteJobId("remote-id");
        job.setStatus(org.open4goods.services.prompt.model.BatchJobStatus.SUBMITTED);
        
        File batchFolder = new File(System.getProperty("java.io.tmpdir") + "/batch-test");
        batchFolder.mkdirs();
        
        try {
            // Manually save the job file
            File file = new File(batchFolder, "batch-job-" + jobId + ".json");
            objectMapper.writeValue(file, job);
            
            // Mock OpenAI client
            when(openAiBatchClient.getBatchStatus("remote-id")).thenReturn(new org.open4goods.services.prompt.dto.openai.BatchJobResponse("id", "batch", "/v1/chat/completions", null, "input-file-id", "24h", "completed", "output-file-id", null, null, null, null, null, null, null, null, null, null, null, null));
            
            // Execute with prefix
            var status = batchPromptService.checkStatus("batch-" + jobId);
            
            assertThat(status).isNotNull();
            assertThat(status.status()).isEqualTo("completed");
            
            // Cleanup
            file.delete();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
