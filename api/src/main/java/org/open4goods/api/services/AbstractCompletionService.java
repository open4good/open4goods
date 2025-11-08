package org.open4goods.api.services;

import org.open4goods.commons.helper.GenericFileLogger;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
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
		logger  = GenericFileLogger.initLogger("completion-"+getClass().getSimpleName().toLowerCase(), logLevel, logFolder);
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
				complete(vConf, withExcluded);
		}
	}



	/**
	 * Proceed to the AI texts generation for a vertical
	 */
	public void complete(VerticalConfig vertical, Integer limit, boolean withExcluded)  {
		logger.info("Generating AI texts for {} products {}",limit == null ? "all" : limit, vertical.getId());
		dataRepository.exportVerticalWithValidDate(vertical, withExcluded).forEach(data -> {
			completeAndIndexProduct(vertical, data);

		});
	}

	public void completeAndIndexProduct(VerticalConfig vertical, Product data) {
		processProduct(vertical, data);
		dataRepository.forceIndex(data);

	}


	/**
	 * Process the item if shouldProcess() is concluant)
	 * @param vertical
	 * @param data
	 */
	public void process(VerticalConfig vertical, Product data) {
		if (shouldProcess(vertical, data)) {
			logger.info("Completing {} with {}",data, this.getClass().getSimpleName());
			processProduct(vertical, data);
		} else {
			logger.info("Skipping completion of {} with {}",data, this.getClass().getSimpleName());
		}
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

	/**
	 * Method that indicates if a product must be completed or not. This will use  Map<String,Integer> datasourceCodes
	 * @param vertical
	 * @param data
	 * @return
	 */
	public abstract boolean shouldProcess(VerticalConfig vertical, Product data);

	/**
	 * Return the completetion datasource name. Used also as key  in dataSourceCodes to maintain arbitrary marker that allows to avoid skip handling if done
	 */

	public abstract String getDatasourceName();


}
