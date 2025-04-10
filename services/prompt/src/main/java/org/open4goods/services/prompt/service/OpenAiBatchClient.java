package org.open4goods.services.prompt.service;

import java.io.File;

import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.open4goods.services.prompt.dto.BatchJobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.core.io.FileSystemResource;

@Component
public class OpenAiBatchClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiBatchClient.class);
    private final RestTemplate restTemplate;
    private final PromptServiceConfig config;

    /**
     * Constructs a new OpenAiBatchClient.
     *
     * @param config the PromptServiceConfig which includes batch API properties.
     */
    public OpenAiBatchClient(PromptServiceConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Submits the batch submission file to the OpenAI Batch API.
     *
     * @param submissionFile the NDJSON file containing batch requests.
     * @return a BatchJobResponse representing the response from OpenAI.
     */
    public BatchJobResponse submitBatch(File submissionFile) {
        String url = config.getBatchApiEndpoint(); // e.g. "https://api.openai.com/v1/batches"
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + config.getOpenaiApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(submissionFile));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<BatchJobResponse> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity,
                BatchJobResponse.class);
        return response.getBody();
    }
}
