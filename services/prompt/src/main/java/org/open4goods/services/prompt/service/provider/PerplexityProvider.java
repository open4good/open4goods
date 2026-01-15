package org.open4goods.services.prompt.service.provider;

import java.util.Map;

import org.open4goods.services.prompt.config.GenAiServiceType;
import org.open4goods.services.prompt.config.PromptOptions;
import org.open4goods.services.prompt.config.RetrievalMode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Perplexity provider using OpenAI-compatible API.
 */
@Component
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
        ChatClientRequestSpec chatRequest = ChatClient.create(new OpenAiChatModel(perplexityApi))
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
}
