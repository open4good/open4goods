package org.open4goods.api.services;

import java.util.List;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.aggregation.aggregator.SanitisationBatchedAggregator;
import org.open4goods.api.services.aggregation.aggregator.ScoringBatchedAggregator;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.product.Product;
import org.open4goods.services.SearchService;
import org.open4goods.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import ch.qos.logback.classic.Level;

/**
 * This service is in charge of various batches
 * @author Goulven.Furet
 */
public class BatchService {



	private static final Logger logger = LoggerFactory.getLogger(BatchService.class);



	private ProductRepository dataRepository;

	private VerticalsConfigService verticalsService;

	private BatchAggregationService batchAggregationService;

	private RealtimeAggregationService realtimeAggregationService;



	private final ApiProperties apiProperties;

	private Logger dedicatedLogger;




	public BatchService(
			RealtimeAggregationService realtimeAggregationService,
			ProductRepository dataRepository,
			ApiProperties apiProperties,
			VerticalsConfigService verticalsService,
			BatchAggregationService batchAggregationService,
			SearchService searchService,
			boolean toConsole
			) {
		super();

		dedicatedLogger = GenericFileLogger.initLogger("stats-batch", Level.INFO, apiProperties.logsFolder(), toConsole);
		this.apiProperties = apiProperties;
		this.dataRepository =dataRepository;
		this.verticalsService=verticalsService;
		this.batchAggregationService=batchAggregationService;
		this.realtimeAggregationService = realtimeAggregationService;
	}

	
	/**
	 * update all verticals. Scheduled
     *
	 */
	@Scheduled( initialDelay = 1000 * 3600*24, fixedDelay = 1000 * 3600*24)
	public void scoreAll()  {
		for (VerticalConfig vertical : verticalsService.getConfigsWithoutDefault()) {
			batchScore(vertical);
		}
	}
	 
	 

	
	/**
	 * Score verticals with the batch Aggragator
	 * @throws AggregationSkipException 
	 */
	public void batchScore(VerticalConfig vertical)  {

		logger.info("Score batching for {}", vertical.getId());

		ScoringBatchedAggregator batchAgg = batchAggregationService.getScoringAggregator(vertical);

		List<Product> productBag = dataRepository.getProductsMatchingCategories(vertical).toList();
		// Batched (scoring) aggregation
		batchAgg.update(productBag);
		logger.info("Score batching : indexing {} products", productBag.size());
		dataRepository.index(productBag);

	}
	

	/**
	 * Launch batch sanitization : on a vertical if specified, on all items if not
	 * Sanitizer aggregator
	 * @throws AggregationSkipException 
	 */
	public void sanitize()  {

			logger.info("started : Sanitisation batching for all items");
			SanitisationBatchedAggregator batchAgg = batchAggregationService.getFullSanitisationAggregator();

			// TODO : Performance, could parallelize
			dataRepository.exportAll().forEach(p -> {
                batchAgg.update(p);
				dataRepository.index(p);
            });
			logger.info("done: Sanitisation batching for all items");			
	}

	/**
	 * Launch the sanitisation of one product
	 * @param product
	 */
	public void sanitizeOne(Product product) {
		logger.info("started : Sanitisation batching for {}",product);
		SanitisationBatchedAggregator batchAgg = batchAggregationService.getFullSanitisationAggregator();

	    batchAgg.update(product);
		dataRepository.index(product);
		logger.info("done : Sanitisation batching for {}", product);		
	}
	


}
