package org.open4goods.services.prompt.service.provider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.open4goods.services.prompt.config.GenAiServiceType;
import org.open4goods.services.prompt.config.PromptOptions;
import org.open4goods.services.prompt.config.RetrievalMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;

/**
 * OpenAI provider implementation.
 */
public class OpenAiProvider implements GenAiProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiProvider.class);
    private static final String RESPONSES_ENDPOINT = "https://api.openai.com/v1/responses";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final OpenAiChatModel chatModel;
    private final Environment environment;

    public OpenAiProvider(OpenAiChatModel chatModel, Environment environment) {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
        this.chatModel = chatModel;
        this.environment = environment;
        logger.info("****************************************************************");
        logger.info("Initializing OpenAiProvider with chatModel: {}", chatModel);
        logger.info("****************************************************************");
    }

    @Override
    public GenAiServiceType service() {
        return GenAiServiceType.OPEN_AI;
    }

    @Override
    public ProviderResult generateText(ProviderRequest request) {
        if (request.getRetrievalMode() == RetrievalMode.MODEL_WEB_SEARCH && request.isAllowWebSearch()) {
            String model = resolveModel(request.getOptions());
            return generateWithWebSearch(request);
        }
        return generateWithChatModel(request);
    }

    @Override
    public Flux<ProviderEvent> generateTextStream(ProviderRequest request) {
        String model = resolveModel(request.getOptions());
        if (request.getRetrievalMode() == RetrievalMode.MODEL_WEB_SEARCH && request.isAllowWebSearch()) {
            return streamFromWebSearch(request, model);
        }
        return streamFromChatModel(request);
    }

    private ProviderResult generateWithChatModel(ProviderRequest request) {
        OpenAiChatOptions options = buildOptions(request.getOptions());
        if (StringUtils.hasText(request.getJsonSchema())) {
            options.setResponseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, request.getJsonSchema()));
        }
        Prompt prompt = buildPrompt(request, options);
        ChatResponse response = chatModel.call(prompt);
        String content = response.getResult().getOutput().getText();
        Map<String, Object> metadata = extractAnnotationsFromResponse(response);
        return new ProviderResult(service(), options.getModel(), content, content, metadata);
    }

    private ProviderResult generateWithWebSearch(ProviderRequest request) {
        try {
            logger.info("Initiating OpenAI web search for prompt: {}", request.getPromptKey());
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
                    .header("Authorization", "Bearer " + resolveApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload));
            applyTimeout(requestBuilder, request.getOptions());
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI responses API failed with status " + response.statusCode());
            }
            String raw = response.body();
            logger.debug("OpenAI Responses API raw output length: {} chars", raw != null ? raw.length() : 0);
            String content = extractContent(raw);
            Map<String, Object> metadata = extractAnnotationsFromRaw(raw);
            int citationCount = metadata.containsKey("citations") ? ((List<?>) metadata.get("citations")).size() : 0;
            logger.info("Extracted {} citations from OpenAI response", citationCount);
            return new ProviderResult(service(), model, raw, content, metadata);
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

    private OpenAiChatOptions buildOptions(PromptOptions options) {
        OpenAiChatOptions chatOptions = new OpenAiChatOptions();
        if (options != null) {
            chatOptions.setModel(resolveModel(options));
            if (options.getTemperature() != null) {
                chatOptions.setTemperature(options.getTemperature());
            } else {
                chatOptions.setTemperature(0.2);  // Default like Gemini
            }
            if (options.getMaxTokens() != null) {
                chatOptions.setMaxTokens(options.getMaxTokens());
            }
            if (options.getTopP() != null) {
                chatOptions.setTopP(options.getTopP());
            } else {
                chatOptions.setTopP(0.9);  // Default like Gemini
            }
            if (options.getSeed() != null) {
                chatOptions.setSeed(options.getSeed());
            }
        } else {
            chatOptions.setModel(resolveModel(null));
            chatOptions.setTemperature(0.2);
            chatOptions.setTopP(0.9);
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

    private Prompt buildPrompt(ProviderRequest request, OpenAiChatOptions options) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        if (StringUtils.hasText(request.getSystemPrompt())) {
            messages.add(new SystemMessage(request.getSystemPrompt()));
        }
        messages.add(new UserMessage(request.getUserPrompt()));
        return new Prompt(messages, options);
    }

    private Flux<ProviderEvent> streamFromChatModel(ProviderRequest request) {
        OpenAiChatOptions options = buildOptions(request.getOptions());
        if (StringUtils.hasText(request.getJsonSchema())) {
            options.setResponseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, request.getJsonSchema()));
        }
        Prompt prompt = buildPrompt(request, options);
        return Flux.defer(() -> {
            StringBuilder content = new StringBuilder();
            Map<String, Object> metadata = new LinkedHashMap<>();
            return Flux.concat(
                    Flux.just(ProviderEvent.started(service(), options.getModel())),
                    chatModel.stream(prompt)
                            .map(response -> {
                                String delta = response.getResult().getOutput().getText();
                                if (StringUtils.hasText(delta)) {
                                    content.append(delta);
                                }
                                Map<String, Object> responseMetadata = extractAnnotationsFromResponse(response);
                                if (!responseMetadata.isEmpty()) {
                                    metadata.putAll(responseMetadata);
                                }
                                if (StringUtils.hasText(delta)) {
                                    return ProviderEvent.streamChunk(service(), options.getModel(), delta);
                                }
                                return null;
                            })
                            .filter(Objects::nonNull),
                    Flux.defer(() -> Flux.just(ProviderEvent.metadata(service(), options.getModel(), metadata))),
                    Flux.defer(() -> Flux.just(ProviderEvent.completed(service(), options.getModel(),
                            content.toString(), metadata)))
            );
        }).onErrorResume(ex -> Flux.just(ProviderEvent.error(service(), options.getModel(), ex.getMessage())));
    }

    private Flux<ProviderEvent> streamFromWebSearch(ProviderRequest request, String model) {
        return Flux.defer(() -> {
            logger.info("Starting streaming web search for model: {}", model);
            ProviderEvent start = ProviderEvent.started(service(), model);
            ProviderEvent toolStart = ProviderEvent.toolStatus(service(), model, "web_search", "started", Map.of());
            ProviderResult result = generateWithWebSearch(request);
            ProviderEvent toolComplete = ProviderEvent.toolStatus(service(), model, "web_search", "completed", Map.of());
            return Flux.concat(
                    Flux.just(start, toolStart, toolComplete),
                    Flux.just(ProviderEvent.streamChunk(service(), model, result.getContent())),
                    Flux.just(ProviderEvent.metadata(service(), model, result.getMetadata())),
                    Flux.just(ProviderEvent.completed(service(), model, result.getContent(), result.getMetadata()))
            );
        }).onErrorResume(ex -> Flux.just(ProviderEvent.error(service(), model, ex.getMessage())));
    }

    private Map<String, Object> extractAnnotationsFromResponse(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return Map.of();
        }
        Map<String, Object> metadata = response.getResult().getOutput().getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Object annotations = metadata.get("annotations");
        if (annotations instanceof List<?> list) {
            List<Map<String, Object>> citations = OpenAiAnnotationParser.parseCitations(list);
            if (!citations.isEmpty()) {
                return Map.of("citations", citations);
            }
        }
        return Map.of();
    }

    private Map<String, Object> extractAnnotationsFromRaw(String rawResponse) {
        try {
            List<Map<String, Object>> citations = OpenAiAnnotationParser.parseCitations(objectMapper, rawResponse);
            if (!citations.isEmpty()) {
                return Map.of("citations", citations);
            }
            return Map.of();
        } catch (Exception e) {
            logger.warn("Failed to parse OpenAI annotations: {}", e.getMessage());
            return Map.of();
        }
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

    private String resolveApiKey() {
        String apiKey = environment.getProperty("spring.ai.openai.api-key");
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Missing spring.ai.openai.api-key for OpenAI web search");
        }
        return apiKey;
    }
}
