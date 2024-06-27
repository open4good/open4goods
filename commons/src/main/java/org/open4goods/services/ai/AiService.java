package org.open4goods.services.ai;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.attributes.PromptConfig;
import org.open4goods.config.yml.attributes.AiPromptsConfig;
import org.open4goods.config.yml.ui.ProductI18nElements;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.data.AiDescription;
import org.open4goods.model.product.Product;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatModel;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TODO : Paralelisation
 * TODO : Storing the prompt in product (disabled for now) could allow to regenerate if changes
 */
public class AiService {

	private final Logger logger = LoggerFactory.getLogger(AiService.class);
	private final OpenAiChatModel chatModel;
	private final VerticalsConfigService verticalService;
	private final EvaluationService spelEvaluationService;
	private ObjectMapper objectMapper;

	public AiService(OpenAiChatModel chatModel, VerticalsConfigService verticalService, EvaluationService spelEvaluationService) {
		this.chatModel = chatModel;
		this.verticalService = verticalService;
		this.spelEvaluationService = spelEvaluationService;
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * Generates a prompt response using the AI model.
	 */
	public String generatePromptResponse(String value) {
		long startTime = System.currentTimeMillis();
		String response = chatModel.call(value);
		logger.info("GenAI request ({}ms) : \n ----------request :\n{} \n----------response\n{}", System.currentTimeMillis() - startTime, value, response);
		return response;
	}

	/**
	 * Completes AI descriptions for a product based on the provided vertical configuration.
	 */
	public void complete(Product product, VerticalConfig verticalConfig, boolean force) {
		// TODO : Gof (filter on icecat validated chars)
		if (!force && product.getAttributes().count() < 15) {
			return;
		}

		logger.info("AI completion for product {}", product.getId());

		Map<String, ProductI18nElements> i18nConfig = verticalConfig.getI18n();
		generateDescriptionsForProduct(product, i18nConfig, force);
	}

	/**
	 * Generates AI descriptions for the given product and configuration.
	 *
	 * @param product The product to generate descriptions for.
	 * @param i18nConfig The internationalization configuration.
	 * @param force Whether to force generation even if not necessary.
	 */
	private void generateDescriptionsForProduct(Product product, Map<String, ProductI18nElements> i18nConfig, boolean force) {
		for (Entry<String, ProductI18nElements> entry : i18nConfig.entrySet()) {
			
			
			
			Map<String, AiDescription> description = createAiDescriptions(entry.getValue().getAiConfigs(), entry.getKey(), product);
			
			
			
			for (PromptConfig aiConfig : entry.getValue().getAiConfigs().getPrompts()) {
				if (shouldSkipDescriptionGeneration(product, aiConfig, i18nConfig, force)) {
					logger.info("Skipping because generated AI text is already present");
					continue;
				}

				if (StringUtils.isBlank(description.getContent().getText())) {
					logger.error("Empty AI text for product {} with key {} and lang {} and prompt {}", product.getId(), aiConfig.getKey(), entry.getKey(), aiConfig.getPrompt());
				} else {
					storeGeneratedDescriptions(product, entry, description);
				}
			}
		}
	}



	/**
	 * Determines if the description generation should be skipped.
	 */
	private boolean shouldSkipDescriptionGeneration(Product product, PromptConfig aiConfig, Map<String, ProductI18nElements> i18nConfig, boolean force) {
		return !force && !aiConfig.isOverride() && product.getAiDescriptions().containsKey(aiConfig.getKey()) && i18nConfig.keySet().contains(product.getAiDescriptions().get(aiConfig.getKey()).getContent().getLanguage());
	}

	/**
	 * Creates an AI description for a product.
	 * @param rootPrompt 
	 *
	 * @param aiConfig The AI configuration.
	 * @param language The language of the description.
	 * @param product The product to generate the description for.
	 * @return The generated AI description.
	 */
	private AiDescription createAiDescriptions(String rootPrompt, PromptConfig aiConfig, String language, Product product) {

		// 1 - Evaluate the root prompt
		
		String evaluatedRootPrompt = spelEvaluationService.thymeleafEval(product, rootPrompt);
		
		
		
		
		
		String evaluatedPrompt = spelEvaluationService.thymeleafEval(product, aiConfig.getPrompt());

		String aiResponse = generatePromptResponse(evaluatedPrompt);
		logger.info("AI response for product {}: \n{}", product.getId(), aiResponse);

		return new AiDescription(aiResponse, language);
	}

	
	private Map<String, AiDescription> createAiDescriptions(AiPromptsConfig aiConfigs, String key, Product product) {

		
		// 1 - Evaluate the root prompt
		String evaluatedRootPrompt = spelEvaluationService.thymeleafEval(product, aiConfigs.getRootPrompt());
		
		// 2 - Evaluate the prompts
		Map<String, String> promptsRepsponse = new HashMap<>();
		for (PromptConfig promptConfig :  aiConfigs.getPrompts()) {
			// For each
			promptsRepsponse.put(promptConfig.getKey(), spelEvaluationService.thymeleafEval(product, promptConfig.getPrompt()));
		}
		
		String template = """
		  given : 
		      {evaluatedRootPrompt}
		  
		  generate a key/value json string, which contains the following entries:
			- global-description : {promptsRepsponse.get("global-description")}
		  
				""";
		
		
		// TODO : chatmodel.execute
		
		
		// TODO : Json parsing
		Map<String, Object> responseMap = objectMapper.readValue(description.getContent().getText(), HashMap.class);

		
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	
	
	
	
	
	/**
	 * Stores the generated AI descriptions in the product.
	 *
	 * @param product The product to store the descriptions in.
	 * @param entry The entry containing the AI configuration.
	 * @param description The AI description to store.
	 */
	private void storeGeneratedDescriptions(Product product, Entry<String, ProductI18nElements> entry, AiDescription description) {
		try {
			Map<String, Object> responseMap = objectMapper.readValue(description.getContent().getText(), HashMap.class);

			logger.info("Parsed AI response for product {}", product.getId());

			product.getAiDescriptions().put("global-description", new AiDescription((String) responseMap.get("global-description"), entry.getKey()));
			product.getAiDescriptions().put("ecological-description", new AiDescription((String) responseMap.get("ecological-description"), entry.getKey()));
		} catch (IOException e) {
			logger.error("Error parsing AI response for product {}: {}", product.getId(), e.getMessage());
		}
	}
}
