package org.open4goods.nudgerfrontapi.service;

import org.open4goods.nudgerfrontapi.config.properties.EmbeddingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Service that proxies semantic embedding calls to the back-office API.
 */
@Service
public class EmbeddingProxyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingProxyService.class);

    private final RestClient restClient;

    public EmbeddingProxyService(RestClient.Builder restClientBuilder, EmbeddingProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.getApiBaseUrl()).build();
    }

    /**
     * Contacts the remote API endpoint to generate an embedding.
     * 
     * @param text The input text to embed.
     * @return The text embedding array.
     */
    public float[] embed(String text) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/product/embedding")
                            .queryParam("text", text)
                            .build())
                    .retrieve()
                    .body(float[].class);
        } catch (Exception e) {
            LOGGER.error("Error fetching embeddings from API for text: {}", text, e);
            throw new RuntimeException("Error fetching embeddings", e);
        }
    }
}
