package org.open4goods.api.services.completion;

import java.util.List;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.aggregation.aggregator.ScoringBatchedAggregator;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.model.product.Product;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.services.ai.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;


public class GenAiCompletionService  {

	protected static final Logger logger = LoggerFactory.getLogger(GenAiCompletionService.class);

	private AiService aiService;

	private ProductRepository dataRepository;
	
	private VerticalsConfigService verticalConfigService;
	
	private ApiProperties apiProperties;
	
	
	public GenAiCompletionService( AiService aiService, ProductRepository dataRepository, VerticalsConfigService verticalConfigService, ApiProperties apiProperties) {
		
		this.aiService = aiService;
		this.dataRepository = dataRepository;
		this.verticalConfigService = verticalConfigService;
		this.apiProperties = apiProperties;
	}


	public void onProduct(Product data, VerticalConfig vConf) {
		aiService.complete(data, vConf);
	}	
	
	/**
	 * Score verticals with the batch Aggregator
	 * TODO : Schedule delay from conf
	 */
	@Scheduled(fixedRate = 1000 * 3600 * 24, initialDelay = 1000 * 3600 * 24)
	public void completeAll()  {
		logger.info("Generating AI texts for all verticals");
		for (VerticalConfig vConf : verticalConfigService.getConfigsWithoutDefault()) {
			if (vConf.getGenAiConfig().isEnabled()) {
				complete(vConf);				
			}
		}
	}
		
	/**
	 * Proceed to the AI texts generation for a vertical
	 * TODO : Limit to the "TOP N / WORSE N"  
	 */
	public void complete(VerticalConfig vertical)  {

		logger.info("Generating AI texts for {}", vertical.getId());

		dataRepository.exportVerticalWithValidDateOrderByEcoscore(vertical.getId(), vertical.getGenAiConfig().getMaxPerVerticals()).forEach(data -> {
			logger.info("AI text completion for {}", data.getId());
			aiService.complete(data, vertical);
			dataRepository.index(data);
		});
	}

}
