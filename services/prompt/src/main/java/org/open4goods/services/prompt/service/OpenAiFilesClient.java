package org.open4goods.services.prompt.service;

import java.io.File;

import org.open4goods.services.prompt.dto.openai.FileUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Client for interacting with the OpenAI Files API.
 */
@Component
public class OpenAiFilesClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiFilesClient.class);
    private final RestTemplate restTemplate;
    private final Environment environment;

    public OpenAiFilesClient(Environment environment) {
        this.environment = environment;
        this.restTemplate = new RestTemplate();
    }

    public FileUploadResponse uploadFile(File file, String purpose) {
        String url = "https://api.openai.com/v1/files";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + resolveApiKey());
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

    /**
     * Downloads the content of a file from the OpenAI Files API.
     *
     * @param fileId the ID of the file to download.
     * @return the content of the file as a String.
     */
    public String downloadFileContent(String fileId) {
        String url = "https://api.openai.com/v1/files/" + fileId + "/content";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + resolveApiKey());
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        String content = response.getBody();
        if (content == null) {
            throw new IllegalStateException("Failed to download content for file " + fileId);
        }
        logger.info("Downloaded content for file {} successfully.", fileId);
        return content;
    }

    private String resolveApiKey() {
        String apiKey = environment.getProperty("spring.ai.openai.api-key");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing spring.ai.openai.api-key for OpenAI files API");
        }
        return apiKey;
    }
}
