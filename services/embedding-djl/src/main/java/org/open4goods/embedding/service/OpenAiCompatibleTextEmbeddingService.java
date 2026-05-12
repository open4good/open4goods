package org.open4goods.embedding.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.List;

import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.ObjectMapper;

/**
 * Text embedding service backed by an OpenAI-compatible /v1/embeddings endpoint.
 */
public class OpenAiCompatibleTextEmbeddingService implements TextEmbeddingService
{
    private static final String EMBEDDINGS_PATH = "/embeddings";

    private final DjlEmbeddingProperties.OpenAiCompatible properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI embeddingsUri;

    public OpenAiCompatibleTextEmbeddingService(DjlEmbeddingProperties properties, ObjectMapper objectMapper)
    {
        this.properties = properties.getOpenai();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.properties.getTimeout())
                .build();
        this.embeddingsUri = URI.create(trimTrailingSlash(this.properties.getBaseUrl()) + EMBEDDINGS_PATH);
    }

    @Override
    public float[] embed(String text)
    {
        if (!StringUtils.hasText(text))
        {
            throw new IllegalArgumentException("Text to embed must not be null or blank");
        }

        List<float[]> vectors = embedBatch(List.of(text));
        if (vectors.isEmpty() || vectors.getFirst() == null)
        {
            throw new IllegalStateException("OpenAI-compatible embedding response did not contain an embedding");
        }
        return vectors.getFirst();
    }

    @Override
    public List<float[]> embedBatch(List<String> texts)
    {
        if (texts == null || texts.isEmpty())
        {
            throw new IllegalArgumentException("Text list to embed must not be null or empty");
        }

        EmbeddingRequest request = new EmbeddingRequest(properties.getModel(), texts);
        EmbeddingResponse response = postEmbeddings(request);
        if (response.data() == null || response.data().isEmpty())
        {
            throw new IllegalStateException("OpenAI-compatible embedding response did not contain data");
        }

        List<float[]> vectors = response.data().stream()
                .sorted(Comparator.comparingInt(EmbeddingData::index))
                .map(EmbeddingData::embedding)
                .toList();
        if (vectors.size() != texts.size())
        {
            throw new IllegalStateException("OpenAI-compatible embedding response size "
                    + vectors.size() + " did not match request size " + texts.size());
        }
        return vectors;
    }

    private EmbeddingResponse postEmbeddings(EmbeddingRequest requestBody)
    {
        try
        {
            String json = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder(embeddingsUri)
                    .timeout(properties.getTimeout())
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
                throw new IllegalStateException("OpenAI-compatible embedding request failed with status "
                        + response.statusCode() + ": " + response.body());
            }
            return objectMapper.readValue(response.body(), EmbeddingResponse.class);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("OpenAI-compatible embedding request failed", e);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI-compatible embedding request was interrupted", e);
        }
    }

    private String trimTrailingSlash(String value)
    {
        if (value.endsWith("/"))
        {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private record EmbeddingRequest(String model, Object input)
    {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EmbeddingResponse(List<EmbeddingData> data, String model)
    {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EmbeddingData(int index, float[] embedding)
    {
    }
}
