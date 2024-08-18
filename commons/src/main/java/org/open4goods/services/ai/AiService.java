package org.open4goods.services.ai;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.attributes.AiPromptsConfig;
import org.open4goods.config.yml.attributes.PromptConfig;
import org.open4goods.config.yml.ui.ProductI18nElements;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.data.AiDescription;
import org.open4goods.model.data.AiDescriptions;
import org.open4goods.model.product.Product;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.SerialisationService;
import org.open4goods.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.micrometer.core.annotation.Timed;

/**
 * This service is in charge of generating texts through GenAI prompts
 */
public class AiService implements HealthIndicator{

	// TODO : From conf
	private static final int MIN_REQUIRED_ATTRIBUTES = 15;
	
	
	
	private final Logger logger = LoggerFactory.getLogger(AiService.class);
	private final OpenAiChatModel chatModel;
	private final EvaluationService spelEvaluationService;
	private final SerialisationService serialisationService;
	
	// Tracked exception for healthcheck
	private Long exceptionCount = 0L;

	public AiService(OpenAiChatModel chatModel,  EvaluationService spelEvaluationService, SerialisationService serialisationService) {
		this.chatModel = chatModel;
		this.spelEvaluationService = spelEvaluationService;
		this.serialisationService = serialisationService;
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
			this.exceptionCount++;
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
					continue;
				}
				
				// Operates the merged prompt generation
				AiDescriptions descriptions = createAiDescriptions(entry.getValue(), entry.getKey(), product);

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
	@SuppressWarnings("unchecked")
	private AiDescriptions createAiDescriptions(AiPromptsConfig aiConfigs, String language, Product product) throws Exception {
		// 1 - Evaluate the root prompt to inject variables
		String evaluatedRootPrompt = spelEvaluationService.thymeleafEval(product, aiConfigs.getRootPrompt());

		// 2 - Evaluate the other prompts
		StringBuilder combinedPrompts = new StringBuilder(evaluatedRootPrompt);
		for (PromptConfig promptConfig : aiConfigs.getPrompts()) {
			combinedPrompts.append("\n").append(promptConfig.getKey()).append(promptConfig.getPrompt());
		}

		// 3 - Generate the final prompt response
		String aiResponse = generatePromptResponse(combinedPrompts.toString());
		
		logger.info("AI response for product {}: \n{}", product.getId(), aiResponse);

		// 4 - Parse the JSON response
		Map<String, String> responseMap;
		
		TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String,String>>() {};
		responseMap = serialisationService.fromJson(aiResponse, typeRef);
		
		// 5 - Create AiDescription objects
		AiDescriptions descriptions = new AiDescriptions();
		for (String key : responseMap.keySet()) {
			descriptions.getDescriptions().put(key, new AiDescription( responseMap.get(key)));
		}

		return descriptions;
	}

	
	/**
	 * Generates a prompt response using the AI model.
	 * @throws Exception 
	 */
	@Timed(value = "GenAiTextsGeneration", description = " direct metric on the  API used  for generation",  extraTags = {"service","ai"})
	public String generatePromptResponse(String value) throws Exception {
		long startTime = System.currentTimeMillis();
		String response = chatModel.call(value);
		logger.info("GenAI request ({}ms) : \n ----------request :\n{} \n----------response\n{}", System.currentTimeMillis() - startTime, value, response);
		
		if (StringUtils.isEmpty(response)) {
			logger.error("Empty response from API, generating for prompt : {}",value);
			throw new Exception("Empty response returned from GENAI Api");
		}
		return response;
	}

	/**
	 * Custom healthcheck, simply goes to DOWN if exception occurs in gen AI process
	 */
	@Override
	public Health health() {
		
		Builder health;
		
		if (0L == exceptionCount) {
			health =  Health.up();
		} else {
			health =  Health.down();
		}
		
		  return health
		            .withDetail("encountered_exceptions" , exceptionCount)
		            .build();
		  
	}
}