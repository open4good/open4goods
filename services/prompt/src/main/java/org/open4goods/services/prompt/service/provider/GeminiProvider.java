package org.open4goods.services.prompt.service.provider;

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
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;

/**
 * Gemini provider implementation using Spring AI Google GenAI with grounding.
 */
@org.springframework.stereotype.Component
public class GeminiProvider implements GenAiProvider {

	private static final Logger logger = LoggerFactory.getLogger(GeminiProvider.class);
	private final VertexAiGeminiChatModel chatModel;

	public GeminiProvider(VertexAiGeminiChatModel chatModel) {
		logger.info("DEBUG: Creating GeminiProvider with chatModel: {}", chatModel);
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
			List<ProviderEvent> startEvents = new ArrayList<>();
			startEvents.add(ProviderEvent.started(service(), options.getModel()));
			if (request.getRetrievalMode() == RetrievalMode.MODEL_WEB_SEARCH && request.isAllowWebSearch()) {
				startEvents.add(ProviderEvent.toolStatus(service(), options.getModel(), "google_search_retrieval", "started",
						Map.of()));
			}
			return Flux.concat(Flux.fromIterable(startEvents),
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
					Flux.defer(() -> {
						if (request.getRetrievalMode() == RetrievalMode.MODEL_WEB_SEARCH && request.isAllowWebSearch()) {
							return Flux.just(ProviderEvent.toolStatus(service(), options.getModel(), "google_search_retrieval", "completed",
									Map.of()));
						}
						return Flux.empty();
					}),
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
			} else {
                builder.temperature(0.2);
            }
			if (options.getTopP() != null) {
				builder.topP(options.getTopP());
			} else {
                builder.topP(0.9);
            }
			if (options.getMaxTokens() != null) {
				builder.maxOutputTokens(options.getMaxTokens());
			}
		} else {
             builder.model("gemini-2.5-pro");
             builder.temperature(0.2);
             builder.topP(0.9);
        }

        // Enable Google Search grounding when requested for interactive prompts.
		if (retrievalMode == RetrievalMode.MODEL_WEB_SEARCH && allowWebSearch) {
            builder.googleSearchRetrieval(true);
		}
		return builder.build();
	}

	private String resolveModel(PromptOptions options) {
		if (options != null && StringUtils.hasText(options.getModel())) {
			return options.getModel();
		}
		return "gemini-2.5-pro";
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
		// ChatGenerationMetadata usually exposes a map-like view. We copy it to ensure mutability.
        Map<String, Object> metadata = new LinkedHashMap<>();
        try {
             response.getResult().getMetadata().entrySet().forEach(e -> metadata.put(e.getKey(), e.getValue()));
             // Explicitly check for grounding keys in the output metadata if they are nested or need lifting
             // For Vertex AI, sometimes metadata is attached to the generation or usage, but Spring AI usually merges them.
        } catch (Exception e) {
        	logger.warn("Error extractGroundingMetadata  ",e);
        }
		return metadata;
	}
}
