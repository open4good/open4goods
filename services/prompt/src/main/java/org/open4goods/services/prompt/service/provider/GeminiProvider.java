package org.open4goods.services.prompt.service.provider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.services.prompt.config.GenAiServiceType;
import org.open4goods.services.prompt.config.PromptOptions;
import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.open4goods.services.prompt.config.RetrievalMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Gemini provider implementation using the Google AI Studio API.
 */
@Component
public class GeminiProvider implements GenAiProvider {

    private static final Logger logger = LoggerFactory.getLogger(GeminiProvider.class);
    private static final String GEMINI_ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final PromptServiceConfig config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiProvider(PromptServiceConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public GenAiServiceType service() {
        return GenAiServiceType.GEMINI;
    }

    @Override
    public ProviderResult generateText(ProviderRequest request) {
        if (!StringUtils.hasText(config.getGeminiApiKey())) {
            throw new IllegalStateException("Gemini API key is missing.");
        }
        try {
            String model = resolveModel(request.getOptions());
            String endpoint = String.format(GEMINI_ENDPOINT_TEMPLATE, model, config.getGeminiApiKey());
            Map<String, Object> body = new LinkedHashMap<>();
            if (StringUtils.hasText(request.getSystemPrompt())) {
                body.put("system_instruction", Map.of(
                        "parts", List.of(Map.of("text", buildSystemPrompt(request)))
                ));
            }
            body.put("contents", List.of(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", request.getUserPrompt()))
            )));
            Map<String, Object> generationConfig = new LinkedHashMap<>();
            PromptOptions options = request.getOptions();
            if (options != null) {
                if (options.getTemperature() != null) {
                    generationConfig.put("temperature", options.getTemperature());
                }
                if (options.getTopP() != null) {
                    generationConfig.put("topP", options.getTopP());
                }
                if (options.getMaxTokens() != null) {
                    generationConfig.put("maxOutputTokens", options.getMaxTokens());
                }
            }
            if (!generationConfig.isEmpty()) {
                body.put("generation_config", generationConfig);
            }
            if (request.getRetrievalMode() == RetrievalMode.MODEL_WEB_SEARCH && request.isAllowWebSearch()) {
                body.put("tools", List.of(Map.of("google_search", Map.of())));
            }
            String payload = objectMapper.writeValueAsString(body);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload));
            applyTimeout(requestBuilder, options);
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("Gemini API failed with status " + response.statusCode());
            }
            String raw = response.body();
            String content = extractContent(raw);
            return new ProviderResult(service(), model, raw, content, Map.of());
        } catch (Exception e) {
            logger.error("Gemini request failed: {}", e.getMessage(), e);
            throw new IllegalStateException("Gemini request failed", e);
        }
    }

    private String extractContent(String rawResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse);
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray() && parts.size() > 0) {
                JsonNode text = parts.get(0).path("text");
                if (text.isTextual()) {
                    return text.asText();
                }
            }
        }
        throw new IllegalStateException("Unable to extract content from Gemini response.");
    }

    private String buildSystemPrompt(ProviderRequest request) {
        if (StringUtils.hasText(request.getJsonSchema())) {
            return request.getSystemPrompt()
                    + "\n\nReturn JSON only and ensure it matches this schema:\n"
                    + request.getJsonSchema();
        }
        return request.getSystemPrompt();
    }

    private String resolveModel(PromptOptions options) {
        if (options != null && StringUtils.hasText(options.getModel())) {
            return options.getModel();
        }
        return "gemini-1.5-pro";
    }

    private void applyTimeout(HttpRequest.Builder requestBuilder, PromptOptions options) {
        if (options != null && options.getTimeoutMs() != null) {
            requestBuilder.timeout(Duration.ofMillis(options.getTimeoutMs()));
        }
    }
}
