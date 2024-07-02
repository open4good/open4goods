package org.open4goods.api.services.completion;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AbstractCompletionService;
import org.open4goods.config.yml.attributes.PromptConfig;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.model.product.Product;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.services.ai.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class GenAiCompletionService  extends AbstractCompletionService{

	protected static final Logger logger = LoggerFactory.getLogger(GenAiCompletionService.class);

	private AiService aiService;
	private ApiProperties apiProperties;
	private PromptConfig aiConfig;
	
	
	public GenAiCompletionService( AiService aiService, ProductRepository dataRepository, VerticalsConfigService verticalConfigService, ApiProperties apiProperties, PromptConfig aiConfig) {
		// TODO : Should set a specific log level here (not "agg(regation)" one)
		super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());		
		this.aiService = aiService;
		this.apiProperties = apiProperties;
		this.aiConfig = aiConfig;
	}

	public void processProduct(VerticalConfig vertical, Product data) {
		logger.info("AI text completion for {}", data.getId());
		aiService.complete(data, vertical, false);
		applyRateLimit();
	}

	private void applyRateLimit() {
		try {
			Thread.sleep(aiConfig.getRateLimitDelay());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.error("Thread interrupted during rate limiting", e);
		}
	}

}
