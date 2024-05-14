package org.open4goods.api.services.completion;

import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.product.Product;
import org.open4goods.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;

import ch.qos.logback.classic.Level;


// TODO : Could be in separate(s) project, to help scaling thos post handlings 
public abstract class AbstractCompletionService {

	protected ProductRepository dataRepository;	
	protected VerticalsConfigService verticalConfigService;
	
	private final Logger logger;
	
	public AbstractCompletionService(ProductRepository dataRepository, VerticalsConfigService verticalConfigService, String logFolder, Level logLevel) {
		super();
		this.dataRepository = dataRepository;
		this.verticalConfigService = verticalConfigService;
		// TODO : handle the toConsole
		logger  = GenericFileLogger.initLogger("completion-"+getClass().getSimpleName().toLowerCase(), logLevel, logFolder, true);
	}

	/**
	 * Score verticals with the batch Aggregator
	 */
	public void completeAll()  {
		completeAll(null);
	}
	
	/**
	 * Score verticals with the batch Aggregator
	 */
	public void completeAll(Integer max)  {
		logger.info("Generating AI texts for all verticals");
		for (VerticalConfig vConf : verticalConfigService.getConfigsWithoutDefault()) {
			if (vConf.getGenAiConfig().isEnabled()) {
				complete(vConf);				
			}
		}
	}
		
	
	
	/**
	 * Proceed to the AI texts generation for a vertical
	 */
	public void complete(VerticalConfig vertical, Integer limit)  {
		logger.info("Generating AI texts for {} products {}",limit == null ? "all" : limit, vertical.getId());
		dataRepository.exportVerticalWithValidDateOrderByEcoscore(vertical.getId(), limit).forEach(data -> {
			completeProduct(vertical, data);
		});
	}

	/**
	 * Proceed to the AI texts generation for a vertical
	 */
	public void complete(VerticalConfig vertical)  {
		 this.complete(vertical,null);
	}
	
	/**
	 * 	
	 * @param vertical
	 * @param data
	 */
	public abstract void completeProduct(VerticalConfig vertical, Product data);

}
