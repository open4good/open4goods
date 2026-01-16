package org.open4goods.services.prompt.service.provider;

import java.util.ArrayList;
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
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;

/**
 * Perplexity provider using OpenAI-compatible API.
 */
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Perplexity provider using OpenAI-compatible API.
 */
@Component
@ConditionalOnProperty(prefix = "gen-ai-config", name = "perplexity-api-key")
public class PerplexityProvider implements GenAiProvider {

    private final OpenAiApi perplexityApi;

    public PerplexityProvider(OpenAiApi perplexityApi) {
        this.perplexityApi = perplexityApi;
    }

    @Override
    public GenAiServiceType service() {
        return GenAiServiceType.PERPLEXITY;
    }

    @Override
    public ProviderResult generateText(ProviderRequest request) {
        if (request.getRetrievalMode() == RetrievalMode.MODEL_WEB_SEARCH && request.isAllowWebSearch()) {
            throw new IllegalStateException("Perplexity provider does not support model-native web search yet.");
        }
        OpenAiChatOptions options = buildOptions(request.getOptions());
        if (StringUtils.hasText(request.getJsonSchema())) {
            options.setResponseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, request.getJsonSchema()));
        }
        OpenAiChatModel chatModel = new OpenAiChatModel(perplexityApi);
        Prompt prompt = buildPrompt(request, options);
        ChatResponse response = chatModel.call(prompt);
        String content = response.getResult().getOutput().getText();
        return new ProviderResult(service(), options.getModel(), content, content, Map.of());
    }

    @Override
    public Flux<ProviderEvent> generateTextStream(ProviderRequest request) {
        if (request.getRetrievalMode() == RetrievalMode.MODEL_WEB_SEARCH && request.isAllowWebSearch()) {
            return Flux.just(ProviderEvent.error(service(), resolveModel(request.getOptions()),
                    "Perplexity provider does not support model-native web search yet."));
        }
        OpenAiChatOptions options = buildOptions(request.getOptions());
        if (StringUtils.hasText(request.getJsonSchema())) {
            options.setResponseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, request.getJsonSchema()));
        }
        OpenAiChatModel chatModel = new OpenAiChatModel(perplexityApi);
        Prompt prompt = buildPrompt(request, options);
        return Flux.defer(() -> {
            StringBuilder content = new StringBuilder();
            return Flux.concat(
                    Flux.just(ProviderEvent.started(service(), options.getModel())),
                    chatModel.stream(prompt)
                            .map(response -> {
                                String delta = response.getResult().getOutput().getText();
                                if (StringUtils.hasText(delta)) {
                                    content.append(delta);
                                    return ProviderEvent.streamChunk(service(), options.getModel(), delta);
                                }
                                return null;
                            })
                            .filter(Objects::nonNull),
                    Flux.defer(() -> Flux.just(ProviderEvent.completed(service(), options.getModel(),
                            content.toString(), Map.of())))
            );
        }).onErrorResume(ex -> Flux.just(ProviderEvent.error(service(), options.getModel(), ex.getMessage())));
    }

    private OpenAiChatOptions buildOptions(PromptOptions options) {
        OpenAiChatOptions chatOptions = new OpenAiChatOptions();
        if (options != null) {
            if (StringUtils.hasText(options.getModel())) {
                chatOptions.setModel(options.getModel());
            }
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
        return "sonar";
    }

    private Prompt buildPrompt(ProviderRequest request, OpenAiChatOptions options) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        if (StringUtils.hasText(request.getSystemPrompt())) {
            messages.add(new SystemMessage(request.getSystemPrompt()));
        }
        messages.add(new UserMessage(request.getUserPrompt()));
        return new Prompt(messages, options);
    }
}
