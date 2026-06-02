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
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.schema.JsonSchemaConverter;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;
import tools.jackson.databind.node.ObjectNode;

/**
 * Gemini provider implementation using Spring AI Google GenAI with grounding.
 * <p>
 * Registered as an explicit bean in
 * {@code org.open4goods.services.prompt.config.AiProvidersConfiguration} (guarded by
 * {@code @ConditionalOnBean(GoogleGenAiChatModel.class)} at method level). It is
 * intentionally NOT a component-scanned {@code @Component}: a class-level
 * {@code @ConditionalOnBean} would be evaluated during component scan, before the
 * auto-configured {@link GoogleGenAiChatModel} bean exists, and the provider would be
 * silently excluded from the {@code ProviderRegistry}.
 * </p>
 */
public class GeminiProvider implements GenAiProvider {

	private static final Logger logger = LoggerFactory.getLogger(GeminiProvider.class);
	private final GoogleGenAiChatModel chatModel;

	public GeminiProvider(GoogleGenAiChatModel chatModel) {
		logger.info("DEBUG: Creating GeminiProvider with chatModel: {}", chatModel);
		this.chatModel = chatModel;
	}

	@Override
	public GenAiServiceType service() {
		return GenAiServiceType.GEMINI;
	}

	@Override
	public ProviderResult generateText(ProviderRequest request) {
		GoogleGenAiChatOptions options = buildOptions(request.getOptions(), request.getRetrievalMode(),
				request.isAllowWebSearch(), request.getJsonSchema());

		ChatResponse response = chatModel.call(buildPrompt(request, options));
		String content = response.getResult().getOutput().getText();
		Map<String, Object> metadata = extractGroundingMetadata(response);
		return new ProviderResult(service(), options.getModel(), content, content, metadata);
	}

	@Override
	public Flux<ProviderEvent> generateTextStream(ProviderRequest request) {
		GoogleGenAiChatOptions options = buildOptions(request.getOptions(), request.getRetrievalMode(),
				request.isAllowWebSearch(), request.getJsonSchema());

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

	private GoogleGenAiChatOptions buildOptions(PromptOptions options, RetrievalMode retrievalMode,
			boolean allowWebSearch, String jsonSchema) {
		GoogleGenAiChatOptions.Builder builder = GoogleGenAiChatOptions.builder();
		String model = options == null ? "gemini-2.5-pro" : resolveModel(options);

		if (options != null) {
			builder.model(model);
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
			builder.model(model);
			builder.temperature(0.2);
			builder.topP(0.9);
		}

		// Gemini 3 supports structured output together with built-in tools. Older
		// models still keep the safer legacy behavior and rely on prompt-only JSON.
		boolean useGrounding = retrievalMode == RetrievalMode.MODEL_WEB_SEARCH && allowWebSearch;

		if (useGrounding) {
			builder.googleSearchRetrieval(true);
			builder.internalToolExecutionEnabled(true);
			logger.info("Enabled Google Search grounding for Gemini model {}", model);
		}
		if (jsonSchema != null && !jsonSchema.isBlank()
				&& (!useGrounding || supportsStructuredOutputWithGrounding(model))) {
			logger.debug("Enabling JSON structured output with Vertex-compatible schema for Gemini");
			builder.responseMimeType("application/json");
			builder.responseSchema(toVertexResponseSchema(jsonSchema));
		} else if (jsonSchema != null && !jsonSchema.isBlank()) {
			logger.info("Skipped Gemini response schema because model {} does not support structured output with "
					+ "grounding in this provider path", model);
		}

		return builder.build();
	}

	private String toVertexResponseSchema(String jsonSchema) {
		try {
			ObjectNode openApiSchema = JsonSchemaConverter.convertToOpenApiSchema(JsonSchemaConverter.fromJson(jsonSchema));
			JsonSchemaGenerator.convertTypeValuesToUpperCase(openApiSchema);
			return openApiSchema.toPrettyString();
		} catch (RuntimeException e) {
			logger.warn("Could not convert JSON schema to Vertex schema format; using original schema", e);
			return jsonSchema;
		}
	}

	private boolean supportsStructuredOutputWithGrounding(String model) {
		return model != null && model.toLowerCase().startsWith("gemini-3");
	}

	private String resolveModel(PromptOptions options) {
		if (options != null && StringUtils.hasText(options.getModel())) {
			return options.getModel();
		}
		return "gemini-2.5-pro";
	}

	private Prompt buildPrompt(ProviderRequest request, GoogleGenAiChatOptions options) {
		List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
		if (StringUtils.hasText(request.getSystemPrompt())) {
			messages.add(new SystemMessage(request.getSystemPrompt()));
		}
		messages.add(new UserMessage(request.getUserPrompt()));
		return new Prompt(messages, options);
	}

	private Map<String, Object> extractGroundingMetadata(ChatResponse response) {
		if (response == null || response.getResult() == null || response.getResult().getMetadata() == null) {
			logger.debug("No metadata available in response");
			return Map.of();
		}
		// ChatGenerationMetadata usually exposes a map-like view. We copy it to ensure mutability.
        Map<String, Object> metadata = new LinkedHashMap<>();
        try {
             response.getResult().getMetadata().entrySet().forEach(e -> metadata.put(e.getKey(), e.getValue()));

             // DEBUG: Log available keys to help verify grounding integration
             if (logger.isDebugEnabled()) {
            	 logger.debug("Gemini Metadata Keys: {}", metadata.keySet());
				 if (metadata.containsKey("groundingMetadata")) {
					 logger.debug("Found groundingMetadata: {}", metadata.get("groundingMetadata"));
				 } else if (metadata.containsKey("grounding_metadata")) {
					 logger.debug("Found grounding_metadata: {}", metadata.get("grounding_metadata"));
					 metadata.put("groundingMetadata", metadata.get("grounding_metadata"));
				 } else {
            		 logger.debug("No groundingMetadata found in response metadata.");
            	 }

				 // Check for search queries in metadata (indicates grounding was triggered)
				 if (metadata.containsKey("webSearchQueries")) {
					 logger.info("Web search queries executed: {}", metadata.get("webSearchQueries"));
				 }
             }
        } catch (Exception e) {
        	logger.warn("Error extractGroundingMetadata  ",e);
        }
		return metadata;
	}
}
