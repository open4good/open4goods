package org.open4goods.services.ai;

import java.util.List;
import java.util.Map.Entry;

import org.open4goods.api.services.aggregation.AbstractBatchAggregationService;
import org.open4goods.config.yml.attributes.AiConfig;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.data.AiDescription;
import org.open4goods.model.product.Product;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.VerticalsConfigService;

/**
 * Service in charge of mapping product categories to verticals
 * @author goulven
 *
 */
public class AiCompletionAggregationService extends AbstractBatchAggregationService {

	private AiService aiService;
	private VerticalsConfigService verticalService;
	private EvaluationService spelEvaluationService;

	public AiCompletionAggregationService( final String logsFolder, final VerticalsConfigService verticalService, AiService aiService, EvaluationService spelEvaluationService,  boolean toConsole) {
		super(logsFolder, toConsole);
		this.verticalService = verticalService;
		this.aiService = aiService;
		this.spelEvaluationService = spelEvaluationService;
		
	}

	@Override
	public void onProduct(Product data) {

		////////////////
		// Getting the config		
		/////////////////

		// We only apply on items having some data quality
		
		// TODO : From config
		if (data.getAttributes().count() < 15 || data.getScores().size() < 3 ) {
			return;
		}
		
		// TODO : Will have to "expire" the generated texts and re generate it, in certain conditions
				
		dedicatedLogger.info("AI completion for product {}", data.getId());
		
		// Getting the config for the category, if any
		VerticalConfig vConf = verticalService.getVerticalForCategories(data.getDatasourceCategories());		
		// defaulting		
		if (null == vConf) {	
				vConf = verticalService.getDefaultConfig();			
		} 
		
		
		if (null != vConf ){
		
			////////////////
			// AI generation		
			/////////////////
			List<AiConfig> aiConfig = vConf.getAiConfig();
			
			for (AiConfig ai : aiConfig) {
				for (Entry<String, String> entry : ai.getPrompts().entrySet()) {
					
					String key = ai.getKey();
					String lang = entry.getKey();
					String value = entry.getValue();
					
					dedicatedLogger.info("AI completion for product {} with key {} and lang {} and value {}", data.getId(), key, lang, value);
					// Passing the spel evaluation
					value = spelEvaluationService.thymeleafEval(data, value);
					
					String aiText = aiService.prompt(value).replace("\n", "");
										
					AiDescription aiDesc = new AiDescription(aiText, lang);
					aiDesc.setPrompt(entry.getValue());
					
					data.getAiDescriptions().put(key,aiDesc );

 					dedicatedLogger.warn("AI completion for product {}-{} : \n{}", data.getId(), key,aiText);
					
				}
				
			}
		}

	}

}
