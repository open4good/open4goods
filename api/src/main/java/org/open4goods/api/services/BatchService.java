package org.open4goods.api.services;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.model.constants.TimeConstants;
import org.open4goods.commons.model.crawlers.FetcherGlobalStats;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * One batch to rule them all
 * 
 * @author Goulven.Furet
 *
 *
 */
public class BatchService {

	protected static final Logger logger = LoggerFactory.getLogger(BatchService.class);

	private VerticalsConfigService verticalsConfigService;

	private CompletionFacadeService completionFacadeService;

	private AggregationFacadeService aggregationFacadeService;

	private ProductRepository dataRepository;
	
	public BatchService(AggregationFacadeService aggregationFacadeService,
			CompletionFacadeService completionFacadeService, VerticalsConfigService verticalsConfigService, ProductRepository dataRepository) {
		super();
		this.aggregationFacadeService = aggregationFacadeService;
		this.completionFacadeService = completionFacadeService;
		this.verticalsConfigService = verticalsConfigService;
		this.dataRepository = dataRepository;

	}

	/**
	 * Operate a clean on all verticals :
	 * > Select all products having a category
	 * > Rematch the vertical
	 * > Save
	 */
//	public void cleanVerticals() {
//		
////		1 - Get all products having vertical
//		
//		dataRepository.getAllHavingVertical().forEach(e -> {
//			VerticalConfig v = verticalsConfigService.getVerticalForCategories(e.getDatasourceCategories());
//			
//			// Unassociating items where we have no mapped categories
//			if (e.getCategoriesByDatasources().size() == 0) {
//				logger.info("Unassociating vertical, no mapped categories for {}", e);
//				e.setVertical(null); 
//				dataRepository.index(e);
//				
//			} else {
//				if (null != v && v.getId().equals(e.getVertical())) {
//					logger.info("No vertical change for {}", e);
//				} else {
//					logger.info("Vertical changed from {} to {} for {}",e.getVertical(),v == null ? "null" : v.getId(),  e);
//					 e.setVertical(v == null ? null : v.getId());
//					 dataRepository.index(e);
//				}
//			}
//		});
//	}
	
	// TODO(p3,conf) : schedule from conf
	@Scheduled(cron = "0 0 13 * * ?")
	public void batch() {
		
		/////////////////////////////////////////////
		// On each vertical, products are in memory loaded
		/////////////////////////////////////////////
		for (VerticalConfig vertical : verticalsConfigService.getConfigsWithoutDefault()) {
			batch(vertical);
		}
		

		logger.info("End of batch");
	}

	/**
	 * Batch a specific vertical
	 * @param vertical
	 */
	public void batch(VerticalConfig vertical) {
		Set<Product> allProducts = new HashSet<>();
		
		logger.info("Loading products in memory for vertical {}", vertical);
		
		// We take all products
		allProducts = 	dataRepository.getProductsMatchingCategoriesOrVerticalId(vertical).collect(Collectors.toSet());
		
		logger.info("Sanitisation of {} products", allProducts);

		////////////////////			
		// We apply simple classification to unmatch products from verticals if needed
		////////////////////			
		aggregationFacadeService.classificationAggregator(vertical, allProducts);
		
		// We filter the products into the "living one" (that have not been unmatched from caegories matching, and that have a valid price			
		Set<Product> products = allProducts.stream()
				.filter(e -> e.getOffersCount().intValue() > 0)
				.filter(e-> null != e.getVertical())
				.collect(Collectors.toSet());
		
		logger.info("Will complete {} products of {}", products.size(), allProducts.size() );
		
		////////////////////		
		//  Launch completion on this products
		////////////////////
		completionFacadeService.processAll(products, vertical);

		logger.info("Will aggregate {} products", products.size());
		//////////////////////////////////
		// Launch aggregation, (now will complete on more Datas (eg API ones)
		/////////////////////////////////
		aggregationFacadeService.aggregateProducts(vertical, allProducts);
		
		
		////////////////////		
		//  Scoring
		////////////////////	
		
		
		try {
			// Scoring
			// We only score the products that are not excluded
			Set<Product> scorable = products.stream().filter(e-> !e.isExcluded()).collect(Collectors.toSet());
			logger.info("Will score {} products on a total of {}", scorable.size(), products.size());
			aggregationFacadeService.score(vertical, scorable);
		} catch (Exception e) {
			logger.error("Error in batch : scoring fail", e);
		}
		
		
		////////////////////		
		//  Persisting
		////////////////////	
		
		logger.info("Adding {} ({} completed) products to indexation",allProducts.size(), products.size());			
		// We flush the queue, no matter the previous fragments, we want to be sure there are no erasure on completed items
		dataRepository.getFullProductQueue().clear();
		dataRepository.addToFullindexationQueue(allProducts);
	}

}
