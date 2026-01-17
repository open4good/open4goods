package org.open4goods.services.prompt.service.provider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.open4goods.services.prompt.config.GenAiServiceType;
import org.open4goods.services.prompt.config.PromptOptions;
import org.open4goods.services.prompt.config.RetrievalMode;
import org.open4goods.services.prompt.service.OpenAiBatchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Gemini provider implementation using Native Vertex AI and Grounding.
 */
@Component
@ConditionalOnProperty(prefix = "gen-ai-config", name = "google-api-json")
public class GeminiProvider implements GenAiProvider {

	private static final Logger logger = LoggerFactory.getLogger(GeminiProvider.class);
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
		VertexAiGeminiChatOptions options = buildOptions(request.getOptions(), request.getRetrievalMode(),
				request.isAllowWebSearch());

		ChatResponse response = chatModel.call(buildPrompt(request, options));
		String content = response.getResult().getOutput().getText();
		Map<String, Object> metadata = extractGroundingMetadata(response);
		return new ProviderResult(service(), options.getModel(), content, content, metadata);
	}

	@Override
	public Flux<ProviderEvent> generateTextStream(ProviderRequest request) {
		VertexAiGeminiChatOptions options = buildOptions(request.getOptions(), request.getRetrievalMode(),
				request.isAllowWebSearch());

		Prompt prompt = buildPrompt(request, options);
		return Flux.defer(() -> {
			StringBuilder content = new StringBuilder();
			Map<String, Object> metadata = new LinkedHashMap<>();
			return Flux.concat(Flux.just(ProviderEvent.started(service(), options.getModel())),
					chatModel.stream(prompt).map(response -> {
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
					}).filter(Objects::nonNull),
					Flux.defer(() -> Flux.just(ProviderEvent.metadata(service(), options.getModel(), metadata))),
					Flux.defer(() -> Flux.just(
							ProviderEvent.completed(service(), options.getModel(), content.toString(), metadata))));
		}).onErrorResume(ex -> Flux.just(ProviderEvent.error(service(), options.getModel(), ex.getMessage())));
	}

	private VertexAiGeminiChatOptions buildOptions(PromptOptions options, RetrievalMode retrievalMode, boolean allowWebSearch) {
		VertexAiGeminiChatOptions.Builder builder = VertexAiGeminiChatOptions.builder();

		if (options != null) {
			builder.model(resolveModel(options));
			if (options.getTemperature() != null) {
				builder.temperature(options.getTemperature());
			}
			if (options.getTopP() != null) {
				builder.topP(options.getTopP());
			}
			if (options.getMaxTokens() != null) {
				builder.maxOutputTokens(options.getMaxTokens());
			}
		} else {
             builder.model("gemini-2.0-flash");
        }

		// Enable Native Grounding
		if (retrievalMode == RetrievalMode.MODEL_WEB_SEARCH && allowWebSearch) {
             builder.googleSearchRetrieval(true);
		}
		return builder.build();
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
		if (response == null || response.getResult() == null || response.getResult().getMetadata() == null) {
			return Map.of();
		}
		// ChatGenerationMetadata usually implements Map or has access to it
        // We'll create a new map to be safe
        Map<String, Object> metadata = new LinkedHashMap<>();
        try {
            // Attempt to treat it as a map or use accessor if known.
            // Spring AI 'ChatGenerationMetadata' is an interface, default impl might just wrap a map.
            // But relying on toString or key-value access is safer.
            // Let's assume it puts everything in the definition.
            // For Vertex AI, grounding info is often in a specific key.
            // We saw earlier that entrySet() worked.
             response.getResult().getMetadata().entrySet().forEach(e -> metadata.put(e.getKey(), e.getValue()));
        } catch (Exception e) {
        	logger.warn("Error extractGroundingMetadata  ",e);
        }
		return metadata;
	}
}
