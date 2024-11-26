package org.open4goods.commons.services.ai;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.commons.config.yml.attributes.AiPromptsConfig;
import org.open4goods.commons.config.yml.attributes.PromptConfig;
import org.open4goods.commons.config.yml.ui.ProductI18nElements;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.model.data.AiDescription;
import org.open4goods.commons.model.data.AiDescriptions;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.EvaluationService;
import org.open4goods.commons.services.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.ParameterizedTypeReference;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.micrometer.core.annotation.Timed;

/**
 * This service is in charge of generating texts through GenAI prompts
 */
public class AiService implements HealthIndicator{

	// TODO(p2,conf) : From conf
	private static final int MIN_REQUIRED_ATTRIBUTES = 15;
	
	
	
	private final Logger logger = LoggerFactory.getLogger(AiService.class);
	private final OpenAiChatModel chatModel;
	private final EvaluationService spelEvaluationService;
	private final SerialisationService serialisationService;
	
	// Tracked exception for healthcheck
	private Long criticalExceptionsCounter = 0L;
	private Long generatedProducts = 0L;
	private Long skippedGenerations = 0L;

	
	
	public AiService(OpenAiChatModel chatModel,  EvaluationService spelEvaluationService, SerialisationService serialisationService) {
		this.chatModel = chatModel;
		this.spelEvaluationService = spelEvaluationService;
		this.serialisationService = serialisationService;
	}

	
	
	
	/**
	 * A prompt with a system message and a user message
	 * @param systemMessage
	 * @param userMessage
	 * @return
	 */
	public String prompt(String systemMessage, String userMessage) {
		
		
		CallResponseSpec ret = ChatClient.create(chatModel).prompt()
				.system(systemMessage)
				.user(userMessage)
				.call();
//				.entity(new ParameterizedTypeReference<Map<String, String>>() {});
		
		return ret.content();
		
			
	}
		

	/**
	 * 
	 * @param prompt
	 * @return
	 */
	public Map<String, String> jsonPrompt(String prompt) {
		
		Map<String,String> ret = ChatClient.create(chatModel).prompt()
				.user(prompt)
				.call()
				.entity(new ParameterizedTypeReference<Map<String, String>>() {});
		
		
		return ret;
	}
	
	public String prompt(String prompt) {
		
		String ret = ChatClient.create(chatModel).prompt()
				.user(prompt)
				.call()
				.content()
				;
		
		
		return ret;
	}
	
	
	
	
	/**
	 * Completes AI descriptions for a product based on the provided vertical configuration, containing the prompts
	 */
	public void complete(Product product, VerticalConfig verticalConfig, boolean force) {
		// Discard if we do not have a minimum set of attributes, but applies if is in forced mode
		if (!force && product.getAttributes().count() < MIN_REQUIRED_ATTRIBUTES) {
			return;
		}

		logger.info("AI completion for product {}", product.getId());

		// Retrieving the required prompts
        Map<String, AiPromptsConfig> iaConfigsPerLanguage = verticalConfig.getI18n().entrySet().stream()
            .collect(Collectors.toMap(
            		Map.Entry<String, ProductI18nElements>::getKey,                     
                entry -> entry.getValue().getAiConfigs()
            ));
		
        // Launch the genAiDescription
		try {
			generateTextsForProduct(product, iaConfigsPerLanguage, force);
		} catch (Exception e) {
			logger.error("Error while generating AI description for product {}", product,e);
			this.criticalExceptionsCounter++;
		}
	}
	
	/**
	 * Generates AI text for the given product and configuration.
	 *
	 * @param product The product to generate descriptions for.
	 * @param iaConfigsPerLanguage The internationalization configuration.
	 * @param force Whether to force generation even if not necessary.
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	private void generateTextsForProduct(Product product, Map<String, AiPromptsConfig> iaConfigsPerLanguage, boolean force) throws Exception {
			// Iterate over each AiPromptsConfig, with language as a key
			for (Entry<String, AiPromptsConfig> entry : iaConfigsPerLanguage.entrySet()) {
				
				// Check if we must proceed
				if (shouldSkipDescriptionGeneration(product, entry.getKey(), entry.getValue() , force)) {
					logger.info("Skipping because generated AI texts are already present for language {}",entry.getKey());
					skippedGenerations++;
					continue;
				}
				
				// Operates the merged prompt generation
				AiDescriptions descriptions = createAiDescriptions(entry.getValue(), entry.getKey(), product,force);

				// Store the generated prompts
				product.getGenaiTexts().put(entry.getKey(), descriptions);
			}

	}
	
	/**
	 * Determines if the description generation should be skipped.
	 */
	private boolean shouldSkipDescriptionGeneration(Product product, String language, AiPromptsConfig promptConfig, boolean force) {
		
		if (force || promptConfig.isOverride()) {
			// It is either forced programaticaly, or forced from conf. Do not skip generation
			return false;
		}
		
		AiDescriptions aiDescriptions = product.getGenaiTexts().get(language);
		if (null == aiDescriptions) {
			// The product does not contains IA Content for the given language, we have to proceed
			return false;
		}
		
		if (!aiDescriptions.getDescriptions().keySet().containsAll(promptConfig.promptKeys())) {
			// There are missing keys. in the stored products. Must regen the "whole" merged prompt
			return false;
		}
		
		// No reason to proceed to generation
		return  true;
	}
	


	/**
	 * Creates AI descriptions for a product, merging the specific prompts in a global prompt
	 *
	 * @param aiConfigs The AI configuration.
	 * @param language The language of the descriptions.
	 * @param product The product to generate the descriptions for.
	 * @return The generated AI descriptions.
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	private AiDescriptions createAiDescriptions(AiPromptsConfig aiConfigs, String language, Product product, boolean force) throws Exception {
		// 1 - Evaluate the root prompt to inject variables
		String evaluatedRootPrompt = spelEvaluationService.thymeleafEval(product, aiConfigs.getRootPrompt());

		// 2 - Evaluate the other prompts
		StringBuilder combinedPrompts = new StringBuilder(evaluatedRootPrompt);
		for (PromptConfig promptConfig : aiConfigs.getPrompts()) {
			
			// We do not add the prompt if we already have a generation on it, unless force == true 
			
			if (force || !product.getGenaiTexts().containsKey(language) || !product.getGenaiTexts().get(language).getDescriptions().containsKey(promptConfig.getKey())) {			
				logger.info("Adding partial prompt {}", promptConfig.getKey());
				combinedPrompts.append("\n").append(promptConfig.getKey()).append(promptConfig.getPrompt());
			} else {
				logger.info("Skipping partial prompt {}", promptConfig.getKey());
			}
		}

		
		
		// 3 - Generate the final prompt response
		String str = combinedPrompts.toString();
		
		// Removing chars that can make the prompt parsing fail 
		str = str.replaceAll("\\{|\\}", "_");
		
		
		Map<String, String> aiResponse = generatePromptResponse(str);
		
		logger.info("Gen AI response for product {}: \n{}", product.getId(), aiResponse);

	
		
		// 5 - Create AiDescription objects
		AiDescriptions descriptions = new AiDescriptions();
		
		
		Set<String> allowedKeys = aiConfigs.getPrompts().stream().map(e -> e.getKey()).collect(Collectors.toSet());
		for (String key : aiResponse.keySet()) {
			
			if (allowedKeys.contains(key)) {
				descriptions.getDescriptions().put(key, new AiDescription( aiResponse.get(key)));				
			} else {
				logger.error("Unexpected key returned by AI : {}:{}",key,aiResponse.get(key));
			}
		}

		return descriptions;
	}

	
	/**
	 * Generates a prompt response using the AI model.
	 * @throws Exception 
	 */
	@Timed(value = "GenAiTextsGeneration", description = " direct metric on the  API used  for generation",  extraTags = {"service","ai"})
	public Map<String, String> generatePromptResponse(String value) throws Exception {
		
		long startTime = System.currentTimeMillis();
		
		
		Map<String,String> ret = ChatClient.create(chatModel).prompt()
									.user(value)
									.call()
									.entity(new ParameterizedTypeReference<Map<String, String>>() {});
		
		
		logger.info("GenAI request ({}ms) : \n ----------request :\n{} \n----------response\n{}", System.currentTimeMillis() - startTime, value, ret);
		
		if (ret == null || ret.size() == 0) {
			logger.error("Empty response from API, generating for prompt : {}",value);
			throw new Exception("Empty response returned from GENAI Api");
		}
		generatedProducts++;
		return ret;
	}

	/**
	 * Custom healthcheck, simply goes to DOWN if critical exception occurs
	 */
	@Override
	public Health health() {
		
		Builder health;
		
		long eCount = criticalExceptionsCounter.longValue();
		
		if (0L == eCount ) {
			health =  Health.up();
		} else {
			health =  Health.down();
		}

		return health
				.withDetail("critical_exceptions", eCount)
				.withDetail("successfull_generations", generatedProducts.longValue())
				.withDetail("skipped_generations", skippedGenerations.longValue())
				.build();
	}







	public OpenAiChatModel getChatModel() {
		return chatModel;
	}








}