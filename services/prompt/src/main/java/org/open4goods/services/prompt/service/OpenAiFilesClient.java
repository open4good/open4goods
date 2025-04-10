package org.open4goods.services.prompt.service;

import java.io.File;

import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.open4goods.services.prompt.dto.FileUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.FileSystemResource;

/**
 * Client for interacting with the OpenAI Files API.
 *
 * <p>
 * This client handles file uploads required by various OpenAI endpoints (e.g., Batch, Fine-tune).
 * It uploads files using multipart/form-data and returns the uploaded file details.
 * </p>
 */
@Component
public class OpenAiFilesClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiFilesClient.class);
    private final RestTemplate restTemplate;
    private final PromptServiceConfig config;

    /**
     * Constructs a new OpenAiFilesClient with the provided configuration.
     *
     * @param config the PromptServiceConfig containing API keys.
     */
    public OpenAiFilesClient(PromptServiceConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Uploads a file to the OpenAI Files API with the specified purpose.
     *
     * <p>
     * The file is uploaded using a multipart/form-data request. The purpose should match the intended use
     * (e.g., "batch" for batch requests).
     * </p>
     *
     * @param file the file to be uploaded.
     * @param purpose the intended purpose of the file (e.g., "batch").
     * @return a FileUploadResponse containing details of the uploaded file.
     * @throws IllegalStateException if the upload fails.
     */
    public FileUploadResponse uploadFile(File file, String purpose) {
        String url = "https://api.openai.com/v1/files"; // File upload endpoint
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + config.getOpenaiApiKey());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));
        body.add("purpose", purpose);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<FileUploadResponse> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, FileUploadResponse.class);
        FileUploadResponse fileResponse = response.getBody();
        if (fileResponse == null) {
            throw new IllegalStateException("File upload failed, null response");
        }
        logger.info("Uploaded file {} with id {}", file.getName(), fileResponse.getId());
        return fileResponse;
    }
}
