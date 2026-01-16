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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;

/**
 * Gemini provider implementation using Google Gemini via OpenAI compatibility.
 */
@Component
public class GeminiProvider implements GenAiProvider {

    private final ChatModel chatModel;

    public GeminiProvider(@Qualifier("geminiChatModel") ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public GenAiServiceType service() {
        return GenAiServiceType.GEMINI;
    }

    @Override
    public ProviderResult generateText(ProviderRequest request) {
        OpenAiChatOptions options = buildOptions(request.getOptions(), request.getRetrievalMode(),
                request.isAllowWebSearch());
        if (StringUtils.hasText(request.getJsonSchema())) {
            options.setResponseFormat(new ResponseFormat(ResponseFormat.Type.JSON_OBJECT, request.getJsonSchema()));
        }
        ChatResponse response = chatModel.call(buildPrompt(request, options));
        String content = response.getResult().getOutput().getText();
        Map<String, Object> metadata = extractGroundingMetadata(response);
        return new ProviderResult(service(), options.getModel(), content, content, metadata);
    }

    @Override
    public Flux<ProviderEvent> generateTextStream(ProviderRequest request) {
        OpenAiChatOptions options = buildOptions(request.getOptions(), request.getRetrievalMode(),
                request.isAllowWebSearch());
         if (StringUtils.hasText(request.getJsonSchema())) {
            // Note: Streaming with JSON schema might behave differently depending on provider
            options.setResponseFormat(new ResponseFormat(ResponseFormat.Type.JSON_OBJECT, request.getJsonSchema()));
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

    private OpenAiChatOptions buildOptions(PromptOptions options, RetrievalMode retrievalMode,
                                                   boolean allowWebSearch) {
        OpenAiChatOptions chatOptions = new OpenAiChatOptions();
        
        if (options != null) {
            chatOptions.setModel(resolveModel(options));
            if (options.getTemperature() != null) {
                chatOptions.setTemperature(options.getTemperature());
            }
            if (options.getTopP() != null) {
                chatOptions.setTopP(options.getTopP());
            }
            if (options.getMaxTokens() != null) {
                chatOptions.setMaxTokens(options.getMaxTokens());
            }
        }

        if (retrievalMode == RetrievalMode.MODEL_WEB_SEARCH && allowWebSearch) {
            // Log warning or implement workaround for Google Search with OpenAI client
        }
        return chatOptions;
    }

    private String resolveModel(PromptOptions options) {
        if (options != null && StringUtils.hasText(options.getModel())) {
            return options.getModel();
        }
        return "gemini-2.0-flash";
    }

    private Prompt buildPrompt(ProviderRequest request, OpenAiChatOptions options) {
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
        // Metadata extraction logic might need adjustment for OpenAI-mapped response
        return Map.of();
    }
}
