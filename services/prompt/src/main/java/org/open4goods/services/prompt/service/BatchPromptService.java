package org.open4goods.services.prompt.service;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.open4goods.model.ai.AiFieldScanner;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.open4goods.services.prompt.dto.BatchPromptResponse;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.dto.openai.BatchJobResponse;
import org.open4goods.services.prompt.dto.openai.BatchOutput;
import org.open4goods.services.prompt.dto.openai.BatchRequestEntry;
import org.open4goods.services.prompt.dto.openai.BatchResponse;
import org.open4goods.services.prompt.exceptions.BatchTokenLimitExceededException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    // Map to track active batch jobs; now the future holds PromptResponse with a List of BatchOutput.
    private final ConcurrentHashMap<String, CompletableFuture<PromptResponse<List<BatchOutput>>>> activeBatchJobs = new ConcurrentHashMap<>();

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
     * @throws BatchTokenLimitExceededException if the total estimated tokens exceed the allowed limit.
     */
    public String batchPromptRequest(String promptKey, List<Map<String, Object>> variablesList, List<String> customIds, Class type) {
        var promptConfig = promptService.getPromptConfig(promptKey);
        if (promptConfig == null) {
            throw new IllegalStateException("Prompt config not found for " + promptKey);
        }
        if (promptConfig.getAiService() != org.open4goods.services.prompt.config.GenAiServiceType.OPEN_AI) {
            throw new IllegalStateException("Batch not supported for " + promptConfig.getAiService() + " (phase 1).");
        }
        if (promptConfig.getRetrievalMode() == org.open4goods.services.prompt.config.RetrievalMode.MODEL_WEB_SEARCH) {
            throw new IllegalStateException("Batch not supported for model-native search prompts.");
        }
        List<BatchRequestEntry> requestEntries = new ArrayList<>();
        int totalEstimatedTokens = 0;
        for (int index = 0; index < variablesList.size(); index++) {
            Map<String, Object> vars = variablesList.get(index);
            BatchRequestEntry entry = createBatchRequestEntry(promptKey, vars, index, customIds.get(index), type);
            requestEntries.add(entry);
            totalEstimatedTokens += estimateTokensFromBatchEntry(entry);
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
            for (BatchRequestEntry entry : requestEntries) {
                String line = objectMapper.writeValueAsString(entry);
                writer.write(line + System.lineSeparator());
            }
        } catch (Exception e) {
            logger.error("Error writing batch submission file: {}", e.getMessage(), e);
            throw new IllegalStateException(e);
        }

        // Submit the batch job.
        BatchJobResponse batchJobResponse = openAiBatchClient.submitBatch(submissionFile);
        logger.info("Submitted batch job to OpenAI: {}", batchJobResponse.id());
        // Optionally, you might want to use the job ID from the response. Here we return our generated jobId.
        return batchJobResponse.id();
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
    public PromptResponse<List<BatchOutput>> batchPromptResponse(String jobId) {
        BatchJobResponse batchStatus = openAiBatchClient.getBatchStatus(jobId);
        logger.debug("Batch job {} status: {}", jobId, batchStatus.status());
        if (!"completed".equalsIgnoreCase(batchStatus.status())) {
            throw new IllegalStateException("Batch job " + jobId + " is not completed yet");
        }
        String outputContent = openAiBatchClient.downloadBatchOutput(jobId);
        // Write output to a local file for record keeping.
        File outputFile = new File(config.getBatchFolder(), "batch-" + jobId + "-output.jsonl");
        try {
            Files.writeString(outputFile.toPath(), outputContent, Charset.defaultCharset());
        } catch (Exception e) {
            logger.error("Error writing output file for job {}: {}", jobId, e.getMessage(), e);
        }

        List<String> lines;
        List<BatchOutput> batchOutputs = new ArrayList<>();
        try {
            lines = Files.readAllLines(outputFile.toPath(), Charset.defaultCharset());
            long submittedAt = 0;
            for (String line : lines) {
                if (StringUtils.hasText(line)) {
                    BatchOutput output = objectMapper.readValue(line, BatchOutput.class);
                    batchOutputs.add(output);
                    if (submittedAt == 0 && output.response() != null && output.response().body() != null) {
                        submittedAt = output.response().body().created();
                    }
                }
            }
            Files.deleteIfExists(outputFile.toPath());
        } catch (Exception e) {
            logger.error("Error processing batch output for job {}: {}", jobId, e.getMessage(), e);
            throw new IllegalStateException(e);
        }
        PromptResponse<List<BatchOutput>> promptResponse = new PromptResponse<>();
        promptResponse.setBody(batchOutputs);
        promptResponse.setRaw(outputContent);
        promptResponse.setStart(batchStatus.createdAt() != null ? batchStatus.createdAt() : System.currentTimeMillis());
        promptResponse.setDuration(System.currentTimeMillis() - promptResponse.getStart());
        return promptResponse;
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
        BatchJobResponse status = openAiBatchClient.getBatchStatus(jobId);
        logger.info("Checked batch status for job {}: {}", jobId, status.status());
        return status;
    }
    
//    
//    /**
//     * Submits a batch prompt request with a list of variable maps.
//     * <p>
//     * Each prompt is fully evaluated and mapped into a NDJSON entry according to the OpenAI Batch API.
//     * The NDJSON file is saved (for recovery) and then submitted via the OpenAiBatchClient.
//     * </p>
//     *
//     * @param promptKey     The key of the prompt template to use.
//     * @param variablesList A list of variable maps for prompt evaluation.
//     * @param customIds     A list of custom IDs corresponding to each prompt entry.
//     * @return a BatchPromptResponse holding the jobId and a Future that will complete when the batch response is available.
//     */
//    public BatchPromptResponse<List<BatchOutput>> batchPrompt(String promptKey, List<Map<String, Object>> variablesList, List<String> customIds) {
//        List<BatchRequestEntry> requestEntries = new ArrayList<>();
//        int totalEstimatedTokens = 0;
//        int index = 0;
//        for (Map<String, Object> vars : variablesList) {
//            BatchRequestEntry entry = createBatchRequestEntry(promptKey, vars, index, customIds.get(index));
//            requestEntries.add(entry);
//            totalEstimatedTokens += estimateTokensFromBatchEntry(entry);
//            index++;
//        }
//        logger.info("Batch prompt {} submitted with total estimated tokens: {}", promptKey, totalEstimatedTokens);
//        if (totalEstimatedTokens > config.getBatchMaxTokens()) {
//            throw new BatchTokenLimitExceededException("Total tokens " + totalEstimatedTokens +
//                    " exceed maximum allowed " + config.getBatchMaxTokens());
//        }
//
//        // Generate a unique internal jobId for this batch submission.
//        String jobId = UUID.randomUUID().toString();
//
//        // Write NDJSON file: one JSON object per request.
//        File batchFolder = new File(config.getBatchFolder());
//        if (!batchFolder.exists() && !batchFolder.mkdirs()) {
//            logger.error("Failed to create batch folder at {}", config.getBatchFolder());
//            throw new IllegalStateException("Cannot create batch folder");
//        }
//        File submissionFile = new File(batchFolder, "batch-" + jobId + "-submission.jsonl");
//        try (FileWriter writer = new FileWriter(submissionFile, Charset.defaultCharset())) {
//            for (BatchRequestEntry entry : requestEntries) {
//                String line = objectMapper.writeValueAsString(entry);
//                writer.write(line + System.lineSeparator());
//            }
//        } catch (Exception e) {
//            logger.error("Error writing batch submission file: {}", e.getMessage(), e);
//            throw new IllegalStateException(e);
//        }
//
//        // Submit the batch job via the OpenAI Batch API.
//        BatchJobResponse batchJobResponse = openAiBatchClient.submitBatch(submissionFile);
//        logger.info("Submitted batch job to OpenAI: {}", batchJobResponse.id());
//
//        // Create a CompletableFuture to be completed when a response is found.
//        CompletableFuture<PromptResponse<List<BatchOutput>>> futureResponse = new CompletableFuture<>();
//        activeBatchJobs.put(jobId, futureResponse);
//
//        return new BatchPromptResponse<>(jobId, futureResponse);
//    }

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

    private int estimateTokensFromBatchEntry(org.open4goods.services.prompt.dto.openai.BatchRequestEntry entry) {
        int tokens = 0;
        for (org.open4goods.services.prompt.dto.openai.BatchMessage m : entry.getBody().getMessages()) {
            tokens += estimateTokens(m.getContent());
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
    public void scanBatchResponses() {
        // Iterate over a copy of job IDs to avoid concurrent modification.
        for (String jobRawId : new ArrayList<>(activeBatchJobs.keySet())) {
            // Convert the raw job id to match the expected batch API format.
//            String jobId = "batch_" + jobRawId.replace("-", "");
        	String jobId = "batch_67f7cde83c8c8190b7386d4179f17efe";
            try {
                BatchJobResponse batchStatus = openAiBatchClient.getBatchStatus(jobId);
                logger.debug("Batch job {} status: {}", jobId, batchStatus.status());

                if ("completed".equalsIgnoreCase(batchStatus.status())) {
                    String outputContent = openAiBatchClient.downloadBatchOutput(jobId);
                    File outputFile = new File(config.getBatchFolder(), "batch-" + jobId + "-output.jsonl");
                    Files.writeString(outputFile.toPath(), outputContent, Charset.defaultCharset());
                    logger.info("Downloaded output for batch job {} to file {}", jobId, outputFile.getName());

                    List<String> lines = Files.readAllLines(outputFile.toPath(), Charset.defaultCharset());
                    List<BatchOutput> batchOutputs = new ArrayList<>();
                    long submittedAt = 0;
                    for (String line : lines) {
                        if (StringUtils.hasText(line)) {
                            // Parse the line into a BatchOutput object.
                            BatchOutput output = objectMapper.readValue(line, BatchOutput.class);
                            batchOutputs.add(output);
                            if (submittedAt == 0 && output.response() != null && output.response().body() != null) {
                                submittedAt = output.response().body().created();
                            }
                        }
                    }

                    CompletableFuture<PromptResponse<List<BatchOutput>>> future = activeBatchJobs.get(jobId);
                    if (future != null) {
                        PromptResponse<List<BatchOutput>> promptResponse = new PromptResponse<>();
                        promptResponse.setBody(batchOutputs);
                        promptResponse.setRaw(outputContent);
                        promptResponse.setStart(submittedAt);
                        promptResponse.setDuration(System.currentTimeMillis() - submittedAt);
                        future.complete(promptResponse);
                        activeBatchJobs.remove(jobId);
                        logger.info("Batch job {} processed and future completed.", jobId);
                    }
                    Files.deleteIfExists(outputFile.toPath());
                } else {
                    logger.error("Batch with uncompleted status: {}", jobId);
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
