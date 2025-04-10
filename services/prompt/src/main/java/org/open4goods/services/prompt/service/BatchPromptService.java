package org.open4goods.services.prompt.service;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.open4goods.services.prompt.dto.BatchJobResponse;
import org.open4goods.services.prompt.dto.BatchPromptResponse;
import org.open4goods.services.prompt.dto.BatchRequestEntry;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.exceptions.BatchTokenLimitExceededException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

/**
 * Service that implements batch prompt functionality based on OpenAI’s JSONL Batch API.
 * <p>
 * This service processes a batch of prompt evaluations, writes them as NDJSON entries
 * to a designated folder, and submits them via the OpenAI Batch API.
 * It also reinitializes pending jobs upon restart and scans for external response files to complete the futures.
 * </p>
 */
@Service
public class BatchPromptService implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(BatchPromptService.class);

    private final PromptServiceConfig config;
    private final EvaluationService evaluationService;
    private final SerialisationService serialisationService;
    private final ObjectMapper objectMapper;
    private final OpenAiBatchClient openAiBatchClient;
    private final PromptService promptService; // for retrieving prompt config and evaluation

    // Map to track active batch jobs by our internal jobId.
    private final ConcurrentHashMap<String, CompletableFuture<PromptResponse<String>>> activeBatchJobs = new ConcurrentHashMap<>();

    /**
     * Constructs a new BatchPromptService.
     *
     * @param config                The prompt service configuration.
     * @param evaluationService     The service used for evaluating prompt templates.
     * @param serialisationService  The service for (de)serialization operations.
     * @param openAiBatchClient     The client for communicating with OpenAI’s Batch API.
     * @param promptService         The prompt service used to retrieve prompt configurations.
     */
    public BatchPromptService(PromptServiceConfig config,
                              EvaluationService evaluationService,
                              SerialisationService serialisationService,
                              OpenAiBatchClient openAiBatchClient,
                              PromptService promptService) {
        this.config = config;
        this.evaluationService = evaluationService;
        this.serialisationService = serialisationService;
        this.openAiBatchClient = openAiBatchClient;
        this.promptService = promptService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Submits a batch prompt request with a list of variable maps.
     * Each prompt is fully evaluated and mapped into a NDJSON entry according to the OpenAI Batch API.
     * The NDJSON file is saved (for recovery) and then submitted via the OpenAiBatchClient.
     *
     * @param promptKey     The key of the prompt template to use.
     * @param variablesList A list of variable maps for prompt evaluation.
     * @return a BatchPromptResponse holding the jobId and a Future that will complete when the batch response is available.
     */
    public BatchPromptResponse<String> batchPrompt(String promptKey, List<Map<String, Object>> variablesList, List<String> customIds) {
        List<BatchRequestEntry> requestEntries = new ArrayList<>();
        int totalEstimatedTokens = 0;
        int index = 0;
        for (Map<String, Object> vars : variablesList) {
            BatchRequestEntry entry = createBatchRequestEntry(promptKey, vars, index, customIds.get(index));
            requestEntries.add(entry);
            totalEstimatedTokens += estimateTokensFromBatchEntry(entry);
            index++;
        }
        logger.info("Batch prompt {} submitted with total estimated tokens: {}", promptKey, totalEstimatedTokens);
        if (totalEstimatedTokens > config.getBatchMaxTokens()) {
            throw new BatchTokenLimitExceededException("Total tokens " + totalEstimatedTokens +
                    " exceed maximum allowed " + config.getBatchMaxTokens());
        }

        // Generate a unique internal jobId for this batch submission.
        String jobId = UUID.randomUUID().toString();

        // Write NDJSON file: one JSON object per request.
        File batchFolder = new File(config.getBatchFolder());
        if (!batchFolder.exists() && !batchFolder.mkdirs()) {
            logger.error("Failed to create batch folder at {}", config.getBatchFolder());
            throw new IllegalStateException("Cannot create batch folder");
        }
        File submissionFile = new File(batchFolder, "batch-" + jobId + "-submission.jsonl");
        try (FileWriter writer = new FileWriter(submissionFile, Charset.defaultCharset())) {
            for (BatchRequestEntry entry : requestEntries) {
                String line = objectMapper.writeValueAsString(entry);
                writer.write(line + System.lineSeparator());
            }
        } catch (Exception e) {
            logger.error("Error writing batch submission file: {}", e.getMessage(), e);
            throw new IllegalStateException(e);
        }

        // Submit the batch job via the OpenAI Batch API.
        BatchJobResponse batchJobResponse = openAiBatchClient.submitBatch(submissionFile);
        logger.info("Submitted batch job to OpenAI: {}", batchJobResponse.getId());

        // Create a CompletableFuture to be completed when a response is found.
        CompletableFuture<PromptResponse<String>> futureResponse = new CompletableFuture<>();
        activeBatchJobs.put(jobId, futureResponse);

        return new BatchPromptResponse<>(jobId, futureResponse);
    }

    /**
     * Creates a BatchRequestEntry by evaluating the prompt fully.
     * It retrieves the prompt configuration and evaluates system and user prompts,
     * then builds the request body (with messages and model).
     *
     * @param promptKey The prompt configuration key.
     * @param variables The map of variables for template evaluation.
     * @param index     The index of the request in the batch.
     * @return a BatchRequestEntry representing the request.
     */
    private BatchRequestEntry createBatchRequestEntry(String promptKey, Map<String, Object> variables, int index, String customId) {
        var promptConfig = promptService.getPromptConfig(promptKey);
        if (promptConfig == null) {
            throw new IllegalStateException("Prompt config not found for " + promptKey);
        }
        String systemEvaluated = "";
        if (promptConfig.getSystemPrompt() != null) {
            systemEvaluated = evaluationService.thymeleafEval(variables, promptConfig.getSystemPrompt());
        }
        String userEvaluated = evaluationService.thymeleafEval(variables, promptConfig.getUserPrompt());

        // Build the message list.
        List<org.open4goods.services.prompt.dto.Message> messages = new ArrayList<>();
        if (StringUtils.hasText(systemEvaluated)) {
            messages.add(new org.open4goods.services.prompt.dto.Message("system", systemEvaluated));
        }
        messages.add(new org.open4goods.services.prompt.dto.Message("user", userEvaluated));

        // Build the request body.
        var body = new org.open4goods.services.prompt.dto.BatchRequestBody();
        if (promptConfig.getOptions() != null && StringUtils.hasText(promptConfig.getOptions().getModel())) {
            body.setModel(promptConfig.getOptions().getModel());
        } else {
            body.setModel("default-model");
        }
        body.setMessages(messages);

        // Create the request entry.
        var entry = new org.open4goods.services.prompt.dto.BatchRequestEntry();
        // Use a generated UUID as the custom_id (alternatively, you can prefix it with the jobId when available)
        entry.setCustomId(customId);
        entry.setMethod("POST");
        entry.setUrl("/v1/chat/completions");
        entry.setBody(body);
        return entry;
    }

    /**
     * Estimates tokens from a BatchRequestEntry by summing tokens in all message contents.
     *
     * @param entry The request entry.
     * @return estimated token count.
     */
    private int estimateTokensFromBatchEntry(org.open4goods.services.prompt.dto.BatchRequestEntry entry) {
        int tokens = 0;
        for (org.open4goods.services.prompt.dto.Message m : entry.getBody().getMessages()) {
            tokens += estimateTokens(m.getContent());
        }
        return tokens;
    }

    /**
     * Simple token estimation: approximates 1.3 tokens per word.
     *
     * @param text The text.
     * @return estimated token count.
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty())
            return 0;
        int words = text.split("\\s+").length;
        return (int) Math.ceil(words * 1.3);
    }

    /**
     * Scheduled task runs every 5 minutes to scan the batch folder for response files.
     * When found, it aggregates responses and completes the corresponding CompletableFuture.
     */
    @Scheduled(fixedDelay = 300_000) // every 5 minutes
    public void scanBatchResponses() {
        File folder = new File(config.getBatchFolder());
        if (!folder.exists() || !folder.isDirectory()) {
            logger.warn("Batch folder {} is not available", config.getBatchFolder());
            return;
        }
        // Look for files named "batch-<jobId>-response.jsonl"
        File[] responseFiles = folder.listFiles((dir, name) -> name.matches("batch-.*-response\\.jsonl$"));
        if (responseFiles == null || responseFiles.length == 0) {
            logger.debug("No batch response files found");
            return;
        }
        for (File responseFile : responseFiles) {
            try {
                // Extract jobId from file name.
                String fileName = responseFile.getName();
                String jobId = fileName.substring("batch-".length(), fileName.indexOf("-response.jsonl"));
                List<String> lines = Files.readAllLines(responseFile.toPath(), Charset.defaultCharset());
                StringBuilder aggregatedResponse = new StringBuilder();
                long submittedAt = 0;
                for (String line : lines) {
                    if (StringUtils.hasText(line)) {
                        JsonNode node = objectMapper.readTree(line);
                        if (aggregatedResponse.length() > 0) {
                            aggregatedResponse.append("\n");
                        }
                        aggregatedResponse.append(node.get("response").asText());
                        if (submittedAt == 0 && node.has("submittedAt")) {
                            submittedAt = node.get("submittedAt").asLong();
                        }
                    }
                }
                CompletableFuture<PromptResponse<String>> future = activeBatchJobs.get(jobId);
                if (future != null) {
                    var promptResponse = new PromptResponse<String>();
                    promptResponse.setRaw(aggregatedResponse.toString());
                    promptResponse.setStart(submittedAt);
                    promptResponse.setDuration(System.currentTimeMillis() - submittedAt);
                    future.complete(promptResponse);
                    activeBatchJobs.remove(jobId);
                    logger.info("Batch job {} completed via file {}", jobId, responseFile.getName());
                    Files.deleteIfExists(responseFile.toPath());
                }
            } catch (Exception e) {
                logger.error("Error processing batch response file {}: {}", responseFile.getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Exposes the status of current batch jobs.
     *
     * @return a BatchStatus DTO containing the number of active jobs and their job IDs.
     */
    public org.open4goods.services.prompt.dto.BatchStatus status() {
        return new org.open4goods.services.prompt.dto.BatchStatus(activeBatchJobs.size(), new ArrayList<>(activeBatchJobs.keySet()));
    }

    /**
     * Health check for the BatchPromptService.
     *
     * @return Health status.
     */
    @Override
    public Health health() {
        File folder = new File(config.getBatchFolder());
        if (!folder.exists() || !folder.isDirectory()) {
            return Health.down().withDetail("error", "Batch folder is not accessible").build();
        }
        return Health.up().build();
    }

    /**
     * Reinitializes pending batch jobs from submission files when the application starts.
     */
    @PostConstruct
    public void init() {
        logger.info("BatchPromptService initialized with batch folder {} and max tokens {}",
                config.getBatchFolder(), config.getBatchMaxTokens());
        File folder = new File(config.getBatchFolder());
        if (folder.exists() && folder.isDirectory()) {
            File[] submissionFiles = folder.listFiles((dir, name) -> name.matches("batch-.*-submission\\.jsonl$"));
            if (submissionFiles != null) {
                for (File file : submissionFiles) {
                    try {
                        String fileName = file.getName();
                        String jobId = fileName.substring("batch-".length(), fileName.indexOf("-submission.jsonl"));
                        activeBatchJobs.putIfAbsent(jobId, new CompletableFuture<>());
                        logger.info("Recovered batch job {} from submission file {}", jobId, fileName);
                    } catch (Exception e) {
                        logger.error("Error recovering batch job from file {}: {}", file.getName(), e.getMessage(), e);
                    }
                }
            }
        }
    }
}
