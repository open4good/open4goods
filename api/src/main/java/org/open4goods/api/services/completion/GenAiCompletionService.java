package org.open4goods.api.services.completion;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AbstractCompletionService;
import org.open4goods.commons.config.yml.attributes.AiPromptsConfig;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.commons.services.ai.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenAiCompletionService  extends AbstractCompletionService{

	protected static final Logger logger = LoggerFactory.getLogger(GenAiCompletionService.class);

	private AiService aiService;

	private ApiProperties apiProperties;
	
	
	public GenAiCompletionService( AiService aiService, ProductRepository dataRepository, VerticalsConfigService verticalConfigService, ApiProperties apiProperties) {
		// TODO : Should set a specific log level here (not "aggregation)" one)
		super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());		
		this.aiService = aiService;
		this.apiProperties = apiProperties;
	}

	public void processProduct(VerticalConfig vertical, Product data) {
		logger.info("AI text completion for {}", data.getId());
		
		// Operating genai completion
		aiService.complete(data, vertical, false);
		
		// Sleeping, to avoid rate limitation
		try {
			Thread.sleep(apiProperties.getGenAiPauseDurationMs());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.error("Thread interrupted during rate limiting", e);
		}
	}
}
