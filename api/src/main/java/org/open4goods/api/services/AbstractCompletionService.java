package org.open4goods.api.services;

import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.helper.GenericFileLogger;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.VerticalsConfigService;
import org.slf4j.Logger;

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
	public void completeAll(boolean withExcluded)  {
		completeAll(null,withExcluded);
	}
	
	/**
	 * Score verticals with the batch Aggregator
	 */
	public void completeAll(Integer max, boolean withExcluded)  {
		logger.info("Completion for all verticals");
		for (VerticalConfig vConf : verticalConfigService.getConfigsWithoutDefault()) {
			if (vConf.getGenAiConfig().isEnabled()) {
				complete(vConf, withExcluded);				
			}
		}
	}
		
	
	
	/**
	 * Proceed to the AI texts generation for a vertical
	 */
	public void complete(VerticalConfig vertical, Integer limit, boolean withExcluded)  {
		logger.info("Generating AI texts for {} products {}",limit == null ? "all" : limit, vertical.getId());
		dataRepository.exportVerticalWithValidDateOrderByEcoscore(vertical.getId(), limit,withExcluded).forEach(data -> {
			completeProduct(vertical, data);
			
		});
	}

	public void completeProduct(VerticalConfig vertical, Product data) {
		processProduct(vertical, data);
		dataRepository.forceIndex(data);
		
	}

	/**
	 * Proceed to the AI texts generation for a vertical
	 */
	public void complete(VerticalConfig vertical,boolean withExcluded)  {
		 this.complete(vertical,null, withExcluded);
	}
	
	/**
	 * 	
	 * @param vertical
	 * @param data
	 */
	public abstract void processProduct(VerticalConfig vertical, Product data);

	
	
	
}
