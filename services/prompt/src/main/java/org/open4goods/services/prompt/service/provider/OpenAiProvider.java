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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * OpenAI provider implementation.
 */
@Component
public class OpenAiProvider implements GenAiProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiProvider.class);
    private static final String RESPONSES_ENDPOINT = "https://api.openai.com/v1/responses";

    private final OpenAiApi openAiApi;
    private final PromptServiceConfig config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiProvider(@org.springframework.beans.factory.annotation.Qualifier("openAiCustomApi") OpenAiApi openAiApi, PromptServiceConfig config) {
        this.openAiApi = openAiApi;
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public GenAiServiceType service() {
        return GenAiServiceType.OPEN_AI;
    }

    @Override
    public ProviderResult generateText(ProviderRequest request) {
        if (request.getRetrievalMode() == RetrievalMode.MODEL_WEB_SEARCH && request.isAllowWebSearch()) {
            return generateWithWebSearch(request);
        }
        return generateWithChatModel(request);
    }

    private ProviderResult generateWithChatModel(ProviderRequest request) {
        OpenAiChatOptions options = buildOptions(request.getOptions());
        if (StringUtils.hasText(request.getJsonSchema())) {
            options.setResponseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, request.getJsonSchema()));
        }
        ChatClientRequestSpec chatRequest = ChatClient.create(new OpenAiChatModel(openAiApi))
                .prompt()
                .user(request.getUserPrompt());
        if (StringUtils.hasText(request.getSystemPrompt())) {
            chatRequest = chatRequest.system(request.getSystemPrompt());
        }
        chatRequest.options(options);
        CallResponseSpec response = chatRequest.call();
        String content = response.content();
        return new ProviderResult(service(), options.getModel(), content, content, Map.of());
    }

    private ProviderResult generateWithWebSearch(ProviderRequest request) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            String model = resolveModel(request.getOptions());
            body.put("model", model);
            body.put("input", buildInput(request));
            body.put("tools", List.of(Map.of("type", "web_search")));
            if (StringUtils.hasText(request.getJsonSchema())) {
                JsonNode schemaNode = objectMapper.readTree(request.getJsonSchema());
                body.put("response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of(
                                "name", "response",
                                "schema", schemaNode,
                                "strict", true
                        )
                ));
            }
            String payload = objectMapper.writeValueAsString(body);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(RESPONSES_ENDPOINT))
                    .header("Authorization", "Bearer " + config.getOpenaiApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload));
            applyTimeout(requestBuilder, request.getOptions());
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI responses API failed with status " + response.statusCode());
            }
            String raw = response.body();
            String content = extractContent(raw);
            return new ProviderResult(service(), model, raw, content, Map.of());
        } catch (Exception e) {
            logger.error("OpenAI web search request failed: {}", e.getMessage(), e);
            throw new IllegalStateException("OpenAI web search request failed", e);
        }
    }

    private List<Map<String, Object>> buildInput(ProviderRequest request) {
        Map<String, Object> user = Map.of(
                "role", "user",
                "content", List.of(Map.of("type", "text", "text", request.getUserPrompt()))
        );
        if (StringUtils.hasText(request.getSystemPrompt())) {
            Map<String, Object> system = Map.of(
                    "role", "system",
                    "content", List.of(Map.of("type", "text", "text", request.getSystemPrompt()))
            );
            return List.of(system, user);
        }
        return List.of(user);
    }

    private String extractContent(String rawResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse);
        JsonNode outputText = root.path("output_text");
        if (outputText.isTextual()) {
            return outputText.asText();
        }
        JsonNode output = root.path("output");
        if (output.isArray() && output.size() > 0) {
            JsonNode content = output.get(0).path("content");
            if (content.isArray() && content.size() > 0) {
                JsonNode text = content.get(0).path("text");
                if (text.isTextual()) {
                    return text.asText();
                }
            }
        }
        throw new IllegalStateException("Unable to extract content from OpenAI response.");
    }

    private OpenAiChatOptions buildOptions(PromptOptions options) {
        OpenAiChatOptions chatOptions = new OpenAiChatOptions();
        if (options != null) {
            chatOptions.setModel(resolveModel(options));
            if (options.getTemperature() != null) {
                chatOptions.setTemperature(options.getTemperature());
            }
            if (options.getMaxTokens() != null) {
                chatOptions.setMaxTokens(options.getMaxTokens());
            }
            if (options.getTopP() != null) {
                chatOptions.setTopP(options.getTopP());
            }
            if (options.getSeed() != null) {
                chatOptions.setSeed(options.getSeed());
            }
        }
        return chatOptions;
    }

    private String resolveModel(PromptOptions options) {
        if (options != null && StringUtils.hasText(options.getModel())) {
            return options.getModel();
        }
        return "gpt-4o-mini";
    }

    private void applyTimeout(HttpRequest.Builder requestBuilder, PromptOptions options) {
        if (options != null && options.getTimeoutMs() != null) {
            requestBuilder.timeout(Duration.ofMillis(options.getTimeoutMs()));
        }
    }
}
