package org.open4goods.services.prompt.service.provider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.open4goods.services.prompt.config.GenAiServiceType;
import org.open4goods.services.prompt.config.PromptOptions;
import org.open4goods.services.prompt.config.RetrievalMode;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;

/**
 * Gemini provider implementation using Vertex AI Gemini.
 */
@Component
public class GeminiProvider implements GenAiProvider {

    private final VertexAiGeminiChatModel chatModel;

    public GeminiProvider(VertexAiGeminiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public GenAiServiceType service() {
        return GenAiServiceType.GEMINI;
    }

    @Override
    public ProviderResult generateText(ProviderRequest request) {
        VertexAiGeminiChatOptions options = buildOptions(request.getOptions(), request.getRetrievalMode(),
                request.isAllowWebSearch());
        if (StringUtils.hasText(request.getJsonSchema())) {
            options.setResponseMimeType("application/json");
            options.setResponseSchema(request.getJsonSchema());
        }
        ChatResponse response = chatModel.call(buildPrompt(request, options));
        String content = response.getResult().getOutput().getText();
        Map<String, Object> metadata = extractGroundingMetadata(response);
        return new ProviderResult(service(), options.getModel(), content, content, metadata);
    }

    @Override
    public Flux<ProviderEvent> generateTextStream(ProviderRequest request) {
        VertexAiGeminiChatOptions options = buildOptions(request.getOptions(), request.getRetrievalMode(),
                request.isAllowWebSearch());
        if (StringUtils.hasText(request.getJsonSchema())) {
            options.setResponseMimeType("application/json");
            options.setResponseSchema(request.getJsonSchema());
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
                                Map<String, Object> responseMetadata = extractGroundingMetadata(response);
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

    private VertexAiGeminiChatOptions buildOptions(PromptOptions options, RetrievalMode retrievalMode,
                                                   boolean allowWebSearch) {
        VertexAiGeminiChatOptions chatOptions = VertexAiGeminiChatOptions.builder().build();
        if (options != null) {
            chatOptions.setModel(resolveModel(options));
            if (options.getTemperature() != null) {
                chatOptions.setTemperature(options.getTemperature());
            }
            if (options.getTopP() != null) {
                chatOptions.setTopP(options.getTopP());
            }
        }
        if (options != null && options.getMaxTokens() != null) {
            chatOptions.setMaxOutputTokens(options.getMaxTokens());
        }
        if (retrievalMode == RetrievalMode.MODEL_WEB_SEARCH && allowWebSearch) {
            chatOptions.setGoogleSearchRetrieval(true);
        }
        return chatOptions;
    }

    private String resolveModel(PromptOptions options) {
        if (options != null && StringUtils.hasText(options.getModel())) {
            return options.getModel();
        }
        return "gemini-2.0-flash";
    }

    private Prompt buildPrompt(ProviderRequest request, VertexAiGeminiChatOptions options) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        if (StringUtils.hasText(request.getSystemPrompt())) {
            messages.add(new SystemMessage(request.getSystemPrompt()));
        }
        messages.add(new UserMessage(request.getUserPrompt()));
        return new Prompt(messages, options);
    }

    private Map<String, Object> extractGroundingMetadata(ChatResponse response) {
        if (response == null || response.getMetadata() == null) {
            return Map.of();
        }
        Object grounding = response.getMetadata().get("groundingMetadata");
        if (grounding == null) {
            grounding = response.getMetadata().get("grounding");
        }
        if (!(grounding instanceof Map<?, ?> groundingMap)) {
            return Map.of();
        }
        Object chunksObject = groundingMap.get("groundingChunks");
        if (!(chunksObject instanceof List<?> chunks)) {
            return Map.of();
        }
        List<Map<String, Object>> citations = new ArrayList<>();
        int index = 1;
        for (Object chunkObject : chunks) {
            if (!(chunkObject instanceof Map<?, ?> chunk)) {
                continue;
            }
            Object web = chunk.get("web");
            if (!(web instanceof Map<?, ?> webMap)) {
                continue;
            }
            String uri = Objects.toString(webMap.get("uri"), null);
            if (!StringUtils.hasText(uri)) {
                continue;
            }
            String title = Objects.toString(webMap.get("title"), null);
            citations.add(Map.of(
                    "number", index++,
                    "title", StringUtils.hasText(title) ? title : uri,
                    "url", uri,
                    "snippet", ""
            ));
        }
        if (citations.isEmpty()) {
            return Map.of();
        }
        return Map.of("citations", citations);
    }
}
