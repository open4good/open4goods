package org.open4goods.services.prompt.service;

import java.io.File;

import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.open4goods.services.prompt.dto.BatchJobResponse;
import org.open4goods.services.prompt.dto.BatchCreateRequest;
import org.open4goods.services.prompt.dto.FileUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * Client for interacting with the OpenAI Batch API.
 *
 * <p>
 * This client first uploads a file containing batch requests using the File API,
 * then creates a batch job using the returned file ID. The file is uploaded with the purpose "batch".
 * </p>
 *
 * <p>
 * <b>Usage:</b> Call {@link #submitBatch(File)} to submit a batch job.
 * </p>
 */
@Component
public class OpenAiBatchClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiBatchClient.class);
    private final RestTemplate restTemplate;
    private final PromptServiceConfig config;
    private final OpenAiFilesClient filesClient;

    /**
     * Constructs a new OpenAiBatchClient with the provided configuration and file client.
     *
     * @param config the PromptServiceConfig containing API keys and endpoints.
     * @param filesClient the OpenAiFilesClient used to upload files.
     */
    public OpenAiBatchClient(PromptServiceConfig config, OpenAiFilesClient filesClient) {
        this.config = config;
        this.filesClient = filesClient;
        this.restTemplate = new RestTemplate();
        // Ensure a Jackson message converter is available to map JSON responses.
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
    }

    /**
     * Submits a batch job to the OpenAI Batch API.
     *
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Uploads the provided NDJSON submission file using the File API with purpose "batch".</li>
     *   <li>Extracts the file ID from the upload response.</li>
     *   <li>Creates a batch job by sending a JSON payload with the file ID to the Batch API endpoint.</li>
     * </ol>
     * </p>
     *
     * @param submissionFile the NDJSON file containing batch requests.
     * @return a BatchJobResponse representing the created batch job.
     * @throws IllegalStateException if the upload or batch creation fails.
     */
    public BatchJobResponse submitBatch(File submissionFile) {
        // Step 1: Upload file and retrieve file ID
        FileUploadResponse uploadResponse = filesClient.uploadFile(submissionFile, "batch");
        String fileId = uploadResponse.getId();

        // Step 2: Build the batch creation request payload.
        // Here we set the endpoint and completion window per OpenAI documentation.
        BatchCreateRequest requestPayload = new BatchCreateRequest(fileId, "/v1/chat/completions", "24h", null);

        String url = config.getBatchApiEndpoint(); // e.g. "https://api.openai.com/v1/batches"
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + config.getOpenaiApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<BatchCreateRequest> requestEntity = new HttpEntity<>(requestPayload, headers);

        ResponseEntity<BatchJobResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity, BatchJobResponse.class);
        BatchJobResponse batchJobResponse = response.getBody();
        if (batchJobResponse == null) {
            throw new IllegalStateException("Batch job submission failed, null response");
        }
        logger.info("Submitted batch job to OpenAI: {}", batchJobResponse.getId());
        return batchJobResponse;
    }
}
