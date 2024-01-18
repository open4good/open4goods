package org.open4goods.services.ai;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.open4goods.config.yml.attributes.AiConfig;
import org.open4goods.config.yml.ui.I18nElements;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.data.AiDescription;
import org.open4goods.model.product.Product;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * TODO : Paralelisation
 */
public class AiService {

	private Logger logger = LoggerFactory.getLogger(AiService.class);
	private AiAgent nudgerAgent;
	private VerticalsConfigService verticalService;
	private EvaluationService spelEvaluationService;

	public AiService(AiAgent customerSupportAgent, final VerticalsConfigService verticalService, EvaluationService spelEvaluationService) {

		this.nudgerAgent = customerSupportAgent;
		this.verticalService = verticalService;
		this.spelEvaluationService = spelEvaluationService;
	
	}

	/**
	 * Prompt the AI
	 * 
	 * @param value
	 * @return
	 */
	public String prompt(String value) {
		
		long now = System.currentTimeMillis();
		
		String ret = nudgerAgent.chat(value);
		logger.info("GenAI request ({}ms} : \n ----------request :\n{} \n----------response\n", System.currentTimeMillis()-now, value, ret);
		return ret;
	}
	
	/**
	 * Complete the product with AI
	 * @param data
	 * @return 
	 */
	public Collection<AiDescription> complete(Product data, VerticalConfig vConf) {
		////////////////
		// Getting the config		
		/////////////////

		// We only apply on items having some data quality
		
		// TODO : From config
		if (data.getAttributes().count() < 15 || data.getScores().size() < 3 ) {
			return new ArrayList<>();
		}
		
		// TODO : Will have to "expire" the generated texts and re generate it, in certain conditions
				
		logger.info("AI completion for product {}", data.getId());
		
			////////////////
			// AI generation		
			/////////////////
			Map<String, I18nElements> aiConfigs = vConf.getI18n();
			
			
			
//			var executorService = Executors.newVirtualThreadPerTaskExecutor();
			// TODO : perf : we could parallelize this
			for (Entry<String, I18nElements> elements : aiConfigs.entrySet()) {
				
				
				for ( AiConfig aic: elements.getValue().getAiConfig()) {
					
					List<AiDescription> desc = generateProductTexts(aic.getKey(), aic.getPrompt(), elements.getKey(), data);
					desc.forEach(d -> data.getAiDescriptions().put(elements.getKey(), d));				
				}
//				executorService.submit(() -> {
					// your code here
					
//				});
			
		}
		return data.getAiDescriptions().values();
	}

	/**
	 * Generate the descriptions for an AiConfig (meaning the localized prompts of an ai config)
	 * @param ai
	 * @param data
	 * @return
	 */
	private List<AiDescription> generateProductTexts( String key, String prompt, String lang, Product data) {
		
		
		// Multi thread / synchronisation
		List<AiDescription> ret = new ArrayList<>();
				
		logger.info("AI completion for product {} with key {} and lang {} and prompt {}", data.getId(), key, lang, prompt);
		// Passing the spel evaluation
		String value = spelEvaluationService.thymeleafEval(data, prompt);
		
		String aiText = prompt(value).replace("\n", "<br/>");
							
		AiDescription aiDesc = new AiDescription(aiText, lang);
		aiDesc.setPrompt(prompt);
		ret.add(aiDesc);
		
		logger.warn("AI completion for product {}-{} : \n{}", data.getId(), key,aiText);
	
		return ret;
	}

}
