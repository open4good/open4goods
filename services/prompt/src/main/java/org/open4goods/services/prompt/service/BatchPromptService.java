package org.open4goods.services.prompt.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.open4goods.model.ai.AiFieldScanner;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.prompt.config.GenAiServiceType;
import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.open4goods.services.prompt.config.VertexBatchConfig;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.dto.BatchResultItem;
import org.open4goods.services.prompt.dto.openai.BatchJobResponse;
import org.open4goods.services.prompt.dto.openai.BatchOutput;
import org.open4goods.services.prompt.dto.openai.BatchRequestEntry;
import org.open4goods.services.prompt.exceptions.BatchTokenLimitExceededException;
import org.open4goods.services.prompt.model.BatchJob;
import org.open4goods.services.prompt.model.BatchJobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

/**
 * Service that implements batch prompt functionality for OpenAI and Vertex Gemini.
 * <p>
 * This service processes a batch of prompt evaluations, writes them as NDJSON entries
 * to a designated folder, and submits them via the provider batch API.
 * It also reinitializes pending jobs upon restart and scans for remote responses.
 * </p>
 */
@Service
public class BatchPromptService implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(BatchPromptService.class);

    private final PromptServiceConfig config;
    private final VertexBatchConfig vertexBatchConfig;
    private final EvaluationService evaluationService;
    private final ObjectMapper objectMapper;
    private final OpenAiBatchClient openAiBatchClient;
    private final VertexGeminiBatchClient vertexGeminiBatchClient;
    private final PromptService promptService; // for retrieving prompt config and evaluation
    private final BatchJobStore jobStore;

    // Map to track active batch jobs.
    private final ConcurrentHashMap<String, BatchJob> activeBatchJobs = new ConcurrentHashMap<>();

    public BatchPromptService(PromptServiceConfig config,
                              VertexBatchConfig vertexBatchConfig,
                              EvaluationService evaluationService,
                              OpenAiBatchClient openAiBatchClient,
                              VertexGeminiBatchClient vertexGeminiBatchClient,
                              PromptService promptService) {
        this.config = config;
        this.vertexBatchConfig = vertexBatchConfig;
        this.evaluationService = evaluationService;
        this.openAiBatchClient = openAiBatchClient;
        this.vertexGeminiBatchClient = vertexGeminiBatchClient;
        this.promptService = promptService;
        this.objectMapper = new ObjectMapper();
        File batchFolder = new File(config.getBatchFolder());
        if (!batchFolder.exists() && !batchFolder.mkdirs()) {
            logger.warn("Failed to create batch folder at {}", batchFolder.getAbsolutePath());
        }
        this.jobStore = new BatchJobStore(batchFolder, objectMapper);
    }






    /**
     * Submits a batch prompt request.
     * <p>
     * This method builds NDJSON entries for each set of prompt variables,
     * writes the submission file, and invokes the external batch API.
     * It returns the generated job ID.
     * </p>
     *
     * @param promptKey     the prompt configuration key.
     * @param variablesList the list of prompt variable maps.
     * @param customIds     the list of custom IDs (typically product identifiers).
     * @return the generated batch job ID.
     * @throws IOException
     * @throws BatchTokenLimitExceededException if the total estimated tokens exceed the allowed limit.
     */
    public String batchPromptRequest(String promptKey, List<Map<String, Object>> variablesList, List<String> customIds, Class type) throws IOException {
        var promptConfig = promptService.getPromptConfig(promptKey);
        if (promptConfig == null) {
            throw new IllegalStateException("Prompt config not found for " + promptKey);
        }
        
        int totalEstimatedTokens = 0;
        List<Object> requestEntries = new ArrayList<>();
        for (int index = 0; index < variablesList.size(); index++) {
            Map<String, Object> vars = variablesList.get(index);
            if (promptConfig.getAiService() == GenAiServiceType.OPEN_AI) {
                BatchRequestEntry entry = createBatchRequestEntry(promptKey, vars, index, customIds.get(index), type);
                requestEntries.add(entry);
                totalEstimatedTokens += estimateTokensFromBatchEntry(entry);
            } else if (promptConfig.getAiService() == GenAiServiceType.GEMINI) {
                Map<String, Object> entry = createVertexBatchEntry(promptKey, vars, customIds.get(index), type);
                requestEntries.add(entry);
                totalEstimatedTokens += estimateTokensFromVertexEntry(entry);
            }
        }
        logger.info("Batch prompt {} submitted with total estimated tokens: {}", promptKey, totalEstimatedTokens);
        if (totalEstimatedTokens > config.getBatchMaxTokens()) {
            throw new BatchTokenLimitExceededException("Total tokens " + totalEstimatedTokens +
                    " exceed maximum allowed " + config.getBatchMaxTokens());
        }

        // Generate a unique job ID (using a UUID in this example).
        String jobId = UUID.randomUUID().toString();

        // Write NDJSON submission file.
        File batchFolder = new File(config.getBatchFolder());
        if (!batchFolder.exists() && !batchFolder.mkdirs()) {
            logger.error("Failed to create batch folder at {}", config.getBatchFolder());
            throw new IllegalStateException("Cannot create batch folder");
        }
        File submissionFile = new File(batchFolder, "batch-" + jobId + "-submission.jsonl");
        try (FileWriter writer = new FileWriter(submissionFile, Charset.defaultCharset())) {
            for (Object entry : requestEntries) {
                String line = objectMapper.writeValueAsString(entry);
                writer.write(line + System.lineSeparator());
            }
        } catch (Exception e) {
            logger.error("Error writing batch submission file: {}", e.getMessage(), e);
            throw new IllegalStateException(e);
        }

        BatchJob job = new BatchJob();
        job.setId(jobId);
        job.setPromptKey(promptKey);
        job.setSubmissionFilePath(submissionFile.getAbsolutePath());
        job.setStatus(BatchJobStatus.SUBMITTED);

        if (promptConfig.getAiService() == GenAiServiceType.OPEN_AI) {
            BatchJobResponse batchJobResponse = openAiBatchClient.submitBatch(submissionFile);
            job.setProvider(GenAiServiceType.OPEN_AI);
            job.setRemoteJobId(batchJobResponse.id());
        } else if (promptConfig.getAiService() == GenAiServiceType.GEMINI) {
            validateVertexBatchConfig();
            job.setProvider(GenAiServiceType.GEMINI);
            String inputUri = vertexGeminiBatchClient.uploadInputFile(jobId, Files.readString(submissionFile.toPath(), Charset.defaultCharset()));
            String remoteJobId = vertexGeminiBatchClient.submitBatchJob("batch-" + jobId,
                    resolveModel(promptConfig.getOptions()), inputUri);
            job.setRemoteJobId(remoteJobId);
        } else {
            throw new IllegalStateException("Batch not supported for " + promptConfig.getAiService());
        }

        jobStore.save(job);
        activeBatchJobs.put(jobId, job);
        logger.info("Submitted batch job {} to provider {}", job.getRemoteJobId(), job.getProvider());
        return jobId;
    }

    /**
     * Processes and returns the batch prompt response.
     * <p>
     * This method checks the external batch job status.
     * If the job is completed, it downloads the output file and parses the batch outputs.
     * </p>
     *
     * @param jobId the job ID to process.
     * @return a PromptResponse containing the list of BatchOutput objects.
     */
    public PromptResponse<List<BatchResultItem>> batchPromptResponse(String jobId) {
        BatchJob job = loadJob(jobId);
        if (job.getProvider() == GenAiServiceType.OPEN_AI) {
            return openAiBatchPromptResponse(job);
        }
        if (job.getProvider() == GenAiServiceType.GEMINI) {
            return geminiBatchPromptResponse(job);
        }
        throw new IllegalStateException("Batch not supported for " + job.getProvider());
    }


    /**
     * Checks the status of a batch job.
     * <p>
     * Calls the OpenAiBatchClient to retrieve the current status of the remote batch job.
     * </p>
     *
     * @param jobId the unique identifier of the batch job.
     * @return the BatchJobResponse representing the current status.
     */
    public BatchJobResponse checkStatus(String jobId) {
        BatchJob job = loadJob(jobId);
        if (job.getProvider() != GenAiServiceType.OPEN_AI) {
            throw new IllegalStateException("Status check is only available for OpenAI jobs.");
        }
        BatchJobResponse status = openAiBatchClient.getBatchStatus(job.getRemoteJobId());
        logger.info("Checked batch status for job {}: {}", job.getRemoteJobId(), status.status());
        return status;
    }

    private BatchRequestEntry createBatchRequestEntry(String promptKey, Map<String, Object> variables, int index, String customId, Class type) {
        var promptConfig = promptService.getPromptConfig(promptKey);
        if (promptConfig == null) {
            throw new IllegalStateException("Prompt config not found for " + promptKey);
        }
        String systemEvaluated = "";
        if (promptConfig.getSystemPrompt() != null) {
            systemEvaluated = evaluationService.thymeleafEval(variables, promptConfig.getSystemPrompt());
        }
        String userEvaluated = evaluationService.thymeleafEval(variables, promptConfig.getUserPrompt());


        // TODO(p2, perf) : cache
        var outputConverter = new BeanOutputConverter<>(type);
        var jsonSchema = outputConverter.getJsonSchema();
        Map<String, String> instructions = AiFieldScanner.getGenAiInstruction(type);

        // Adding json fields instructions
        if (instructions.size() > 0) {
        	systemEvaluated +="\n. En complément du schéma JSON, voici les instructions concernant chaque champs que tu dois fournir.\n";
        	for (Entry<String, String> entry : instructions.entrySet()) {
        		systemEvaluated+=entry.getKey() + " : " + entry.getValue()+"\n";
        	}
        }

        if (!StringUtils.isEmpty(jsonSchema)) {
        	systemEvaluated+="\n\n Output response format must strictly follow the following json schema : \n\n";
        	systemEvaluated+=jsonSchema + "\n\n";
        }




        // Build the message list.
        List<org.open4goods.services.prompt.dto.openai.BatchMessage> messages = new ArrayList<>();
        if (StringUtils.hasText(systemEvaluated)) {
            messages.add(new org.open4goods.services.prompt.dto.openai.BatchMessage("system", systemEvaluated));
        }
        messages.add(new org.open4goods.services.prompt.dto.openai.BatchMessage("user", userEvaluated));

        // Build the request body.
        var body = new org.open4goods.services.prompt.dto.openai.BatchRequestBody();
        if (promptConfig.getOptions() != null && StringUtils.hasText(promptConfig.getOptions().getModel())) {
            body.setModel(promptConfig.getOptions().getModel());
        } else {
            body.setModel("default-model");
        }
        body.setMessages(messages);

        // Create the request entry.
        var entry = new org.open4goods.services.prompt.dto.openai.BatchRequestEntry();
        entry.setCustomId(customId);
        entry.setMethod("POST");
        entry.setUrl("/v1/chat/completions");
        entry.setBody(body);
        return entry;
    }

    private Map<String, Object> createVertexBatchEntry(String promptKey, Map<String, Object> variables, String customId, Class type) {
        var promptConfig = promptService.getPromptConfig(promptKey);
        if (promptConfig == null) {
            throw new IllegalStateException("Prompt config not found for " + promptKey);
        }
        String systemEvaluated = "";
        if (promptConfig.getSystemPrompt() != null) {
            systemEvaluated = evaluationService.thymeleafEval(variables, promptConfig.getSystemPrompt());
        }
        String userEvaluated = evaluationService.thymeleafEval(variables, promptConfig.getUserPrompt());

        var outputConverter = new BeanOutputConverter<>(type);
        var jsonSchema = outputConverter.getJsonSchema();
        Map<String, String> instructions = AiFieldScanner.getGenAiInstruction(type);
        if (!instructions.isEmpty()) {
            systemEvaluated += "\n. En complément du schéma JSON, voici les instructions concernant chaque champs que tu dois fournir.\n";
            for (Entry<String, String> entry : instructions.entrySet()) {
                systemEvaluated += entry.getKey() + " : " + entry.getValue() + "\n";
            }
        }
        if (StringUtils.hasText(jsonSchema)) {
            systemEvaluated += "\n\n Output response format must strictly follow the following json schema : \n\n";
            systemEvaluated += jsonSchema + "\n\n";
        }

        Map<String, Object> toolMap = null;
        if (promptConfig.getRetrievalMode() == org.open4goods.services.prompt.config.RetrievalMode.MODEL_WEB_SEARCH) {
             toolMap = Map.of("google_search", Map.of());
        }

        Map<String, Object> contentMap = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userEvaluated))
        );

        Map<String, Object> systemMap = Map.of(
                 "parts", List.of(Map.of("text", systemEvaluated))
        );

        Map<String, Object> requestMap = new java.util.HashMap<>();
        requestMap.put("custom_id", customId);
        requestMap.put("contents", List.of(contentMap));
        requestMap.put("system_instruction", systemMap);
        
        if (toolMap != null) {
            requestMap.put("tools", List.of(toolMap));
        }

        return requestMap;
    }

    private int estimateTokensFromBatchEntry(org.open4goods.services.prompt.dto.openai.BatchRequestEntry entry) {
        int tokens = 0;
        for (org.open4goods.services.prompt.dto.openai.BatchMessage m : entry.getBody().getMessages()) {
            tokens += estimateTokens(m.getContent());
        }
        return tokens;
    }

    private int estimateTokensFromVertexEntry(Map<String, Object> entry) {
        int tokens = 0;
        Object contents = entry.get("contents");
        if (contents instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Object parts = map.get("parts");
                    tokens += estimateTokensFromParts(parts);
                }
            }
        }
        Object systemInstruction = entry.get("system_instruction");
        if (systemInstruction instanceof Map<?, ?> map) {
            tokens += estimateTokensFromParts(map.get("parts"));
        }
        return tokens;
    }

    private int estimateTokensFromParts(Object parts) {
        if (!(parts instanceof List<?> list)) {
            return 0;
        }
        int tokens = 0;
        for (Object part : list) {
            if (part instanceof Map<?, ?> partMap) {
                Object text = partMap.get("text");
                tokens += estimateTokens(text != null ? text.toString() : null);
            }
        }
        return tokens;
    }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty())
            return 0;
        int words = text.split("\\s+").length;
        return (int) Math.ceil(words * 1.3);
    }

    /**
     * Scheduled task runs every 5 minutes to scan active batch jobs.
     * <p>
     * For each active job, it checks the batch status via the OpenAiBatchClient. If the job’s status is "completed",
     * it downloads the output file (saved locally with a "-output" suffix), parses each line into a BatchOutput object,
     * aggregates them into a list, completes the corresponding CompletableFuture with a PromptResponse containing the list,
     * and then cleans up the file.
     * </p>
     */
    @Scheduled(fixedDelayString = "${gen-ai-config.batch-poll-interval:PT30S}")
    public void scanBatchResponses() {
        // Iterate over a copy of job IDs to avoid concurrent modification.
        for (String jobId : new ArrayList<>(activeBatchJobs.keySet())) {
            try {
                BatchJob job = loadJob(jobId);
                if (job.getProvider() == GenAiServiceType.OPEN_AI) {
                    BatchJobResponse batchStatus = openAiBatchClient.getBatchStatus(job.getRemoteJobId());
                    logger.debug("Batch job {} status: {}", job.getRemoteJobId(), batchStatus.status());
                    if ("completed".equalsIgnoreCase(batchStatus.status())) {
                        job.setStatus(BatchJobStatus.COMPLETED);
                        jobStore.save(job);
                        activeBatchJobs.remove(jobId);
                    } else if ("in_progress".equalsIgnoreCase(batchStatus.status())
                            || "running".equalsIgnoreCase(batchStatus.status())) {
                        job.setStatus(BatchJobStatus.RUNNING);
                        jobStore.save(job);
                    }
                } else if (job.getProvider() == GenAiServiceType.GEMINI) {
                    JsonNode status = vertexGeminiBatchClient.getJobStatus(job.getRemoteJobId());
                    String state = status.path("state").asText();
                    if ("JOB_STATE_SUCCEEDED".equalsIgnoreCase(state)) {
                        job.setStatus(BatchJobStatus.COMPLETED);
                        jobStore.save(job);
                        activeBatchJobs.remove(jobId);
                    } else if ("JOB_STATE_RUNNING".equalsIgnoreCase(state)) {
                        job.setStatus(BatchJobStatus.RUNNING);
                        jobStore.save(job);
                    } else if ("JOB_STATE_FAILED".equalsIgnoreCase(state) || "JOB_STATE_CANCELLED".equalsIgnoreCase(state)) {
                        job.setStatus(BatchJobStatus.FAILED);
                        job.setErrorMessage(status.path("error").path("message").asText());
                        jobStore.save(job);
                        activeBatchJobs.remove(jobId);
                    }
                }
            } catch (Exception e) {
                logger.error("Error processing batch job {}: {}", jobId, e.getMessage(), e);
            }
        }
    }

    public org.open4goods.services.prompt.dto.openai.BatchStatus status() {
        return new org.open4goods.services.prompt.dto.openai.BatchStatus(activeBatchJobs.size(), new ArrayList<>(activeBatchJobs.keySet()));
    }

    @Override
    public Health health() {
        File folder = new File(config.getBatchFolder());
        if (!folder.exists() || !folder.isDirectory()) {
            return Health.down().withDetail("error", "Batch folder is not accessible").build();
        }
        return Health.up().build();
    }

    @PostConstruct
    public void init() {
        logger.info("BatchPromptService initialized with batch folder {} and max tokens {}",
                config.getBatchFolder(), config.getBatchMaxTokens());
        File folder = new File(config.getBatchFolder());
        if (folder.exists() && folder.isDirectory()) {
            for (BatchJob job : jobStore.loadAll()) {
                if (job.getStatus() == BatchJobStatus.SUBMITTED || job.getStatus() == BatchJobStatus.RUNNING) {
                    activeBatchJobs.put(job.getId(), job);
                    logger.info("Recovered batch job {} from store", job.getId());
                }
            }
        }
    }

    private PromptResponse<List<BatchResultItem>> openAiBatchPromptResponse(BatchJob job) {
        BatchJobResponse batchStatus = openAiBatchClient.getBatchStatus(job.getRemoteJobId());
        logger.debug("Batch job {} status: {}", job.getRemoteJobId(), batchStatus.status());
        if (!"completed".equalsIgnoreCase(batchStatus.status())) {
            throw new IllegalStateException("Batch job " + job.getRemoteJobId() + " is not completed yet");
        }
        job.setStatus(BatchJobStatus.COMPLETED);
        jobStore.save(job);
        String outputContent = openAiBatchClient.downloadBatchOutput(job.getRemoteJobId());
        File outputFile = new File(config.getBatchFolder(), "batch-" + job.getId() + "-output.jsonl");
        try {
            Files.writeString(outputFile.toPath(), outputContent, Charset.defaultCharset());
        } catch (Exception e) {
            logger.error("Error writing output file for job {}: {}", job.getId(), e.getMessage(), e);
        }
        List<BatchResultItem> outputs = new ArrayList<>();
        long submittedAt = batchStatus.createdAt() != null ? batchStatus.createdAt() : System.currentTimeMillis();
        try {
            List<String> lines = Files.readAllLines(outputFile.toPath(), Charset.defaultCharset());
            for (String line : lines) {
                if (StringUtils.hasText(line)) {
                    BatchOutput output = objectMapper.readValue(line, BatchOutput.class);
                    String content = output.response().body().choices().getFirst().message().getContent();
                    outputs.add(new BatchResultItem(output.customId(), content, line, Map.of()));
                }
            }
            Files.deleteIfExists(outputFile.toPath());
        } catch (Exception e) {
            logger.error("Error processing batch output for job {}: {}", job.getId(), e.getMessage(), e);
            throw new IllegalStateException(e);
        }
        PromptResponse<List<BatchResultItem>> promptResponse = new PromptResponse<>();
        promptResponse.setBody(outputs);
        promptResponse.setRaw(outputContent);
        promptResponse.setStart(submittedAt);
        promptResponse.setDuration(System.currentTimeMillis() - promptResponse.getStart());
        return promptResponse;
    }

    private PromptResponse<List<BatchResultItem>> geminiBatchPromptResponse(BatchJob job) {
        JsonNode status = vertexGeminiBatchClient.getJobStatus(job.getRemoteJobId());
        String state = status.path("state").asText();
        if (!"JOB_STATE_SUCCEEDED".equalsIgnoreCase(state)) {
            throw new IllegalStateException("Vertex batch job " + job.getRemoteJobId() + " is not completed yet");
        }
        job.setStatus(BatchJobStatus.COMPLETED);
        jobStore.save(job);
        String outputUriPrefix = status.path("outputInfo").path("gcsOutputDirectory").asText();
        String outputContent = vertexGeminiBatchClient.downloadBatchOutput(outputUriPrefix);
        File outputFile = new File(config.getBatchFolder(), "batch-" + job.getId() + "-output.jsonl");
        try {
            Files.writeString(outputFile.toPath(), outputContent, Charset.defaultCharset());
        } catch (Exception e) {
            logger.error("Error writing output file for job {}: {}", job.getId(), e.getMessage(), e);
        }
        List<BatchResultItem> outputs = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(outputFile.toPath(), Charset.defaultCharset());
            for (String line : lines) {
                if (StringUtils.hasText(line)) {
                    JsonNode node = objectMapper.readTree(line);
                    String customId = node.path("custom_id").asText();
                    String content = node.path("predictions").path(0).path("content").asText();
                    outputs.add(new BatchResultItem(customId, content, line, Map.of()));
                }
            }
            Files.deleteIfExists(outputFile.toPath());
        } catch (Exception e) {
            logger.error("Error processing Vertex batch output for job {}: {}", job.getId(), e.getMessage(), e);
            throw new IllegalStateException(e);
        }
        PromptResponse<List<BatchResultItem>> promptResponse = new PromptResponse<>();
        promptResponse.setBody(outputs);
        promptResponse.setRaw(outputContent);
        promptResponse.setStart(System.currentTimeMillis());
        promptResponse.setDuration(System.currentTimeMillis() - promptResponse.getStart());
        return promptResponse;
    }

    private BatchJob loadJob(String jobId) {
        return jobStore.load(jobId).orElseThrow(() -> new IllegalStateException("Batch job not found: " + jobId));
    }

    private String resolveModel(org.open4goods.services.prompt.config.PromptOptions options) {
        if (options != null && StringUtils.hasText(options.getModel())) {
            return options.getModel();
        }
        return "publishers/google/models/gemini-1.5-flash-001";
    }

    private void validateVertexBatchConfig() {
        if (!StringUtils.hasText(vertexBatchConfig.getProjectId())
                || !StringUtils.hasText(vertexBatchConfig.getBucket())
                || !StringUtils.hasText(vertexBatchConfig.getCredentialsJson())) {
            throw new IllegalStateException("Vertex batch configuration is missing (vertex.batch.project-id, bucket, credentials-json).");
        }
    }
}
