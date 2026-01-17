package org.open4goods.services.prompt.service;

import java.io.File;

import org.open4goods.services.prompt.dto.openai.BatchCreateRequest;
import org.open4goods.services.prompt.dto.openai.BatchJobResponse;
import org.open4goods.services.prompt.dto.openai.FileUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client for interacting with the OpenAI Batch API.
 * <p>
 * This client handles file uploads and batch job creation as well as status checking and result retrieval.
 * </p>
 */
@Component
public class OpenAiBatchClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiBatchClient.class);
    private final RestTemplate restTemplate;
    private final OpenAiFilesClient filesClient;
    private final Environment environment;

    public OpenAiBatchClient(OpenAiFilesClient filesClient, Environment environment) {
        this.filesClient = filesClient;
        this.restTemplate = new RestTemplate();
        this.environment = environment;
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
    }

    public BatchJobResponse submitBatch(File submissionFile) {
        // (Existing implementation remains unchanged)
        FileUploadResponse uploadResponse = filesClient.uploadFile(submissionFile, "batch");
        String fileId = uploadResponse.getId();
        BatchCreateRequest requestPayload = new BatchCreateRequest(fileId, "/v1/chat/completions", "24h", null);
        String url = resolveBatchEndpoint();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + resolveApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<BatchCreateRequest> requestEntity = new HttpEntity<>(requestPayload, headers);
        ResponseEntity<BatchJobResponse> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, BatchJobResponse.class);
        BatchJobResponse batchJobResponse = response.getBody();
        if (batchJobResponse == null) {
            throw new IllegalStateException("Batch job submission failed, null response");
        }
        logger.info("Submitted batch job to OpenAI: {}", batchJobResponse.id());
        return batchJobResponse;
    }

    /**
     * Retrieves the status of a batch job from the Batch API.
     *
     * @param jobId the internal job ID.
     * @return the BatchJobResponse representing the current status.
     */
    public BatchJobResponse getBatchStatus(String jobId) {
        String url = resolveBatchEndpoint() + "/" + jobId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + resolveApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<BatchJobResponse> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, BatchJobResponse.class);
        BatchJobResponse statusResponse = response.getBody();
        if (statusResponse == null) {
            throw new IllegalStateException("Failed to retrieve batch status for job " + jobId);
        }
        logger.info("Retrieved batch status for job {}: {}", jobId, statusResponse.status());
        return statusResponse;
    }

    /**
     * Downloads the output of a completed batch job.
     *
     * @param jobId the internal job ID.
     * @return the output file content as a String.
     */
    public String downloadBatchOutput(String jobId) {
        // Get the batch status to retrieve the output file ID.
        BatchJobResponse statusResponse = getBatchStatus(jobId);
        String outputFileId = statusResponse.outputFileId();
        if (outputFileId == null || outputFileId.isEmpty()) {
            throw new IllegalStateException("Output file ID is not available for job " + jobId);
        }
        // Use the files client to download the file content.
        return filesClient.downloadFileContent(outputFileId);
    	
    }

    private String resolveBatchEndpoint() {
        String endpoint = environment.getProperty("spring.ai.openai.batch.endpoint");
        if (endpoint != null && !endpoint.isBlank()) {
            return endpoint;
        }
        endpoint = environment.getProperty("gen-ai-config.batch-api-endpoint");
        if (endpoint != null && !endpoint.isBlank()) {
            return endpoint;
        }
        return "https://api.openai.com/v1/batches";
    }

    private String resolveApiKey() {
        String apiKey = environment.getProperty("spring.ai.openai.api-key");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing spring.ai.openai.api-key for OpenAI batch");
        }
        return apiKey;
    }
}
