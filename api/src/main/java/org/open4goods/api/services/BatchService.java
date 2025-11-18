package org.open4goods.api.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.open4goods.services.feedservice.service.FeedService;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.crawler.services.fetching.CsvDatasourceFetchingService;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

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

	private CsvDatasourceFetchingService csvDatasourceFetchingService;

	private FeedService feedService;

	private ProductRepository dataRepository;

	private SerialisationService serialisationService;


	public BatchService(AggregationFacadeService aggregationFacadeService,
			CompletionFacadeService completionFacadeService, VerticalsConfigService verticalsConfigService, ProductRepository dataRepository, CsvDatasourceFetchingService csvDatasourceFetchingService, FeedService feedService, SerialisationService serialisationService) {
		super();
		this.aggregationFacadeService = aggregationFacadeService;
		this.completionFacadeService = completionFacadeService;
		this.verticalsConfigService = verticalsConfigService;
		this.dataRepository = dataRepository;
		this.csvDatasourceFetchingService = csvDatasourceFetchingService;
		this.feedService = feedService;
		this.serialisationService = serialisationService;

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

		// We take all products that are typed with the given vertical
		allProducts = 	dataRepository.getProductsMatchingVerticalId(vertical).collect(Collectors.toSet());

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





	///////////////////////
	///// Feeds retrieving
	///////////////////////
	///





	/**
     * Fetches all feeds by aggregating datasource properties from all feed services and orphan configurations,
     * then starting the fetching process for each datasource.
     */
    @Scheduled(cron = "19 13 23 * * ?") // For example, schedule daily at 1 AM.
    public void fetchFeeds() {
        logger.info("Initiating full feed fetching process.");
        Set<DataSourceProperties> datasources = feedService.getFeedsUrl();

        List<DataSourceProperties> datasourceList = new ArrayList<>(datasources);
        long seed = System.nanoTime();
        java.util.Collections.shuffle(datasourceList, new java.util.Random(seed));

        logger.info("Total feeds to fetch: {}", datasourceList.size());
        for (DataSourceProperties ds : datasourceList) {
            try {
                logger.info("Fetching feed: {}", ds);
                csvDatasourceFetchingService.start(ds, ds.getDatasourceConfigName());
            } catch (Exception e) {
                logger.error("Error fetching feed {}: ", ds, e);
            }
        }
    }



    /**
     * Fetches feeds that match the specified feed URL.
     *
     * @param url the feed URL to match
     */
    public void fetchFeedsByUrl(String url) {
        logger.info("Fetching feeds with URL: {}", url);
        Set<DataSourceProperties> datasources =  feedService.getFeedsUrl();
        logger.info("Found {} feeds for processing.", datasources.size());

        for (DataSourceProperties ds : datasources) {
            try {
                if (ds.getCsvDatasource().getDatasourceUrls().contains(url)) {
                    logger.info("Fetching feed: {}", ds);
                    csvDatasourceFetchingService.start(ds, ds.getDatasourceConfigName());
                } else {
                    logger.debug("Skipping feed: {}", ds);
                }
            } catch (Exception e) {
                logger.error("Error fetching feed {}: ", ds, e);
            }
        }
    }

    /**
     * Fetches feeds that match the specified feed key.
     *
     * @param feedKey the feed key to match
     */
    public void fetchFeedsByKey(String feedKey) {
        logger.info("Fetching feeds with key: {}", feedKey);
        Set<DataSourceProperties> datasources = matchingKey(feedKey);
        for (DataSourceProperties ds : datasources) {
            try {
                logger.info("Fetching feed {}: {}", ds.getDatasourceConfigName(), ds);
                csvDatasourceFetchingService.start(ds, ds.getDatasourceConfigName());
            } catch (Exception e) {
                logger.error("Error fetching feed {}: ", ds.getDatasourceConfigName(), e);
            }
        }
    }



    /**
     * Filters and returns datasource properties that match the provided feed key.
     *
     * @param feedKey the feed key to match
     * @return a set of matching datasource properties
     */
    private Set<DataSourceProperties> matchingKey(String feedKey) {
        String cleanedKey = IdHelper.azCharAndDigits(feedKey).toLowerCase();
        Set<DataSourceProperties> result = new HashSet<>();
        for (DataSourceProperties ds : feedService.getFeedsUrl()) {
            try {
                String configName = IdHelper.azCharAndDigits(ds.getDatasourceConfigName()).toLowerCase();
                String dsName = IdHelper.azCharAndDigits(ds.getName()).toLowerCase();
                if (cleanedKey.equals(configName) || cleanedKey.equals(dsName)) {
                    result.add(ds);
                    logger.info("Matched feed: {}", ds);
                }
            } catch (Exception e) {
                logger.error("Error matching feed {}: ", ds, e);
            }
        }
        return result;
    }


    // TODO(p3,design) : remove
	public void clean() {
		AtomicInteger counter = new AtomicInteger();
		dataRepository.exportAll().forEach(p -> {
			int i = counter.incrementAndGet();
			if (i % 1000 == 0) {
				logger.warn("Batched items : {}", i);
			}
			String textVersion = null;
			try {
				textVersion = serialisationService.toJson(p);
				if (textVersion.contains("openfoodfacts")) {
					logger.error("Will remove {}", p);
					dataRepository.delete(p);
				}

			} catch (SerialisationException e) {
				e.printStackTrace();
			}

		});


	}



}
