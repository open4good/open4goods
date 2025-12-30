package org.open4goods.nudgerfrontapi.service.http;

import org.open4goods.commons.services.TextEmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@link TextEmbeddingService} implementation delegating to the embedding gateway over HTTP.
 */
public class EmbeddingGatewayClient implements TextEmbeddingService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingGatewayClient.class);

    private final RestClient restClient;

    public EmbeddingGatewayClient(RestClient restClient)
    {
        this.restClient = restClient;
    }

    @Override
    public float[] embed(String text)
    {
        if (!StringUtils.hasText(text))
        {
            throw new IllegalArgumentException("Text to embed must not be blank");
        }

        try
        {
            EmbeddingResponse response = restClient.post()
                    .uri("/embeddings/text")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new EmbeddingRequest(text))
                    .retrieve()
                    .body(EmbeddingResponse.class);

            if (response == null)
            {
                LOGGER.warn("Embedding gateway returned an empty response for text input");
                return null;
            }
            return response.embedding();
        }
        catch (RestClientException ex)
        {
            LOGGER.warn("Failed to fetch embedding from gateway: {}", ex.getMessage());
            return null;
        }
    }

    private record EmbeddingRequest(String text)
    {
    }

    private record EmbeddingResponse(float[] embedding)
    {
    }
}
