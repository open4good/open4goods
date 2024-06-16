package org.open4goods.services.ai;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.attributes.AiConfig;
import org.open4goods.config.yml.ui.ProductI18nElements;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.data.AiDescription;
import org.open4goods.model.product.Product;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiImageModel;
/**
 * TODO : Paralelisation
 * TODO : Storing the prompt in product (disabled for now) could allow to regenerate if changes
 */
public class AiService {

	private Logger logger = LoggerFactory.getLogger(AiService.class);
    private final OpenAiChatModel chatModel;
	private VerticalsConfigService verticalService;
	private EvaluationService spelEvaluationService;
	
	public AiService(OpenAiChatModel chatModel, final VerticalsConfigService verticalService, EvaluationService spelEvaluationService) {
		this.chatModel = chatModel;
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
		String ret =chatModel.call(value);
		logger.info("GenAI request ({}ms} : \n ----------request :\n{} \n----------response\n", System.currentTimeMillis()-now, value, ret);
		return ret;
	}

	/**
	 * Complete the product with AI
	 * @param data
	 * @param force
	 * @return
	 */
	public void complete(Product data, VerticalConfig vConf, boolean force) {
		////////////////
		// Getting the config
		/////////////////

		// We only apply on items having some data quality

		// TODO : From config
		if (!force && data.getAttributes().count() < 15 ) {
			return;
		}

		// TODO : Will have to "expire" the generated texts and re generate it, in certain conditions

		logger.info("AI completion for product {}", data.getId());

			////////////////
			// AI generation
			/////////////////
			Map<String, ProductI18nElements> i18nConf = vConf.getI18n();

//			var executorService = Executors.newVirtualThreadPerTaskExecutor();
//			executor.execute(() -> {
				logger.info("Started generation for {}",data);
				doGeneration(data, i18nConf,force);
				logger.info("Ended generation for {}",data);
//			});
	}

	private void doGeneration(Product data, Map<String, ProductI18nElements> i18nConf, boolean force) {
		for (Entry<String, ProductI18nElements> elements : i18nConf.entrySet()) {

			for ( AiConfig aic: elements.getValue().getAiConfig()) {

				if (!force && !aic.isOverride() &&  (null != data.getAiDescriptions().get(aic.getKey()) && i18nConf.keySet().contains(data.getAiDescriptions().get(aic.getKey()).getContent().getLanguage()))) {
					logger.info("Skipping because generated AI text is already present");
					continue;
				}

				AiDescription desc = generateProductTexts(aic.getKey(), aic.getPrompt(), elements.getKey(), data);
				if (StringUtils.isEmpty(desc.getContent().getText())) {
					logger.error("Empty AI text for product {} with key {} and lang {} and prompt {}", data.getId(), aic.getKey(), elements.getKey(), aic.getPrompt());
				} else {
					data.getAiDescriptions().put(aic.getKey(), desc);
				}
			}
		}
	}

	/**
	 * Generate the descriptions for an AiConfig (meaning the localized prompts of an ai config)
	 * @param ai
	 * @param data
	 * @return
	 */
	private AiDescription generateProductTexts( String key, String prompt, String lang, Product data) {


		// Multi thread / synchronisation
		List<AiDescription> ret = new ArrayList<>();

		logger.info("AI completion for product {} with key {} and lang {} and prompt {}", data.getId(), key, lang, prompt);
		// Passing the spel evaluation
		String value = spelEvaluationService.thymeleafEval(data, prompt);

		// TODO : should remove the replace
		String aiText = prompt(value).replace("\n", "<br/>");


		AiDescription aiDesc = new AiDescription(aiText, lang);
		ret.add(aiDesc);

		logger.warn("AI completion for product {}-{} : \n{}", data.getId(), key,aiText);

		return aiDesc;
	}

}
