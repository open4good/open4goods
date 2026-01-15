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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Gemini provider implementation using OpenAI compatibility layer.
 */
@Component
public class GeminiProvider implements GenAiProvider {

    private final OpenAiApi geminiApi;

    public GeminiProvider(@Qualifier("geminiApi") OpenAiApi geminiApi) {
        this.geminiApi = geminiApi;
    }

    @Override
    public GenAiServiceType service() {
        return GenAiServiceType.GEMINI;
    }

    @Override
    public ProviderResult generateText(ProviderRequest request) {
        if (request.getRetrievalMode() == RetrievalMode.MODEL_WEB_SEARCH && request.isAllowWebSearch()) {
             // Note: Google Search tool via OpenAI compat layer might need specific handling or might not be supported directly.
             // For now, we will proceed with the standard chat model generation.
             // If tool support is strictly required, additional tool definitions/calling logic would be needed here.
        }
        
        OpenAiChatOptions options = buildOptions(request.getOptions());
        if (StringUtils.hasText(request.getJsonSchema())) {
            options.setResponseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, request.getJsonSchema()));
        }

        ChatClientRequestSpec chatRequest = ChatClient.create(new OpenAiChatModel(geminiApi))
                .prompt()
                .user(request.getUserPrompt());

        if (StringUtils.hasText(request.getSystemPrompt())) {
            chatRequest = chatRequest.system(request.getSystemPrompt());
        }
        chatRequest.options(options);

        // TODO: Handle Google Search tool insertion if supported via OpenAI tools protocol or legacy extensions
        
        CallResponseSpec response = chatRequest.call();
        String content = response.content();
        return new ProviderResult(service(), options.getModel(), content, content, Map.of());
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
        return "gemini-1.5-pro";
    }
}
