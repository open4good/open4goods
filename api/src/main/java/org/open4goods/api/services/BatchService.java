package org.open4goods.api.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.open4goods.aggregation.aggregator.BatchedAggregator;
import org.open4goods.aggregation.aggregator.RealTimeAggregator;
import org.open4goods.api.config.yml.ApiProperties;
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

import com.google.common.collect.Lists;

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
			batchUpdate(vertical);
		}
	}
	 
	 

	/**
	 * Update a vertical
	 * @param verticalId
	 */
	public void fullUpdate(String verticalId) {
		VerticalConfig vertical = verticalsService.getConfigById(verticalId).orElseThrow();
		fullUpdate(vertical);
	}
	
	/**
	 * Update verticals with the batch Aggragator
	 * @throws AggregationSkipException 
	 */
	public void fullUpdate(VerticalConfig vertical)  {

		logger.info("Full update for {}", vertical.getId());
		BatchedAggregator batchAgg = batchAggregationService.getAggregator(vertical);
		RealTimeAggregator rtAgg = realtimeAggregationService.getAggregator(vertical);
		
		Stream<Product> productsStream = dataRepository.getProductsMatchingVertical(vertical);
		
		List<Product> productBag = new ArrayList<>();
		logger.info("Starting realtime aggregation");
		// Realtime aggregation
		productsStream.forEach(data -> {
			try {
				dedicatedLogger.debug("Realtime aggregation for {}", data);
				//TODO : Bad design
				productBag.add( rtAgg.build(data.getFragment(), data));
			} catch (AggregationSkipException e) {
				dedicatedLogger.warn("Error on realtimeaggregation aggregation for {}", data, e);
			}
		});
		
		dedicatedLogger.info("Starting batch aggregation");
		// Batched (scoring) aggregation
		batchAgg.update(productBag);
		
		// TODO : Bulk size from conf
		Lists.partition(productBag, 200).forEach(p -> {
			dedicatedLogger.info("Indexing {} products", p.size());
			dataRepository.index(p);
		});

	}
	
	
	
	/**
	 * Update verticals with the batch Aggragator
	 * @throws AggregationSkipException 
	 */
	public void batchUpdate(VerticalConfig vertical)  {

		logger.info("Batch update for {}", vertical.getId());

		BatchedAggregator batchAgg = batchAggregationService.getAggregator(vertical);

		List<Product> productBag = dataRepository.getProductsMatchingVertical(vertical).toList();
		// Batched (scoring) aggregation
		batchAgg.update(productBag);
		// TODO : Bulk size from conf
		Lists.partition(productBag, 200).forEach(p -> {
			logger.info("Indexing {} products", p.size());
			dataRepository.index(p);
		});

	}
	




}
