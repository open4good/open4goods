package org.open4goods.api.services;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.Iterator;
import java.util.stream.Stream;

import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.attribute.ProductAttribute;

import org.open4goods.services.feedservice.service.FeedService;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.services.feedservice.service.FeedIndexingService;
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

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.model.resource.Resource;
import org.open4goods.model.resource.ResourceType;
import org.apache.commons.io.FileUtils;

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

	private FeedIndexingService feedIndexingService;

	private FeedService feedService;

	private ProductRepository dataRepository;

	private SerialisationService serialisationService;

	private ResourceService resourceService;

	private ApiProperties apiProperties;


	public BatchService(AggregationFacadeService aggregationFacadeService,
			CompletionFacadeService completionFacadeService, VerticalsConfigService verticalsConfigService, ProductRepository dataRepository, FeedIndexingService feedIndexingService, FeedService feedService, SerialisationService serialisationService, ResourceService resourceService, ApiProperties apiProperties) {
		super();
		this.aggregationFacadeService = aggregationFacadeService;
		this.completionFacadeService = completionFacadeService;
		this.verticalsConfigService = verticalsConfigService;
		this.dataRepository = dataRepository;
		this.feedIndexingService = feedIndexingService;
		this.feedService = feedService;
		this.serialisationService = serialisationService;
		this.resourceService = resourceService;
		this.apiProperties = apiProperties;
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

	/**
	 * Scans the local cached resources directory, compares the files against
	 * all active resources registered in the product repository, and moves
	 * orphaned cache files (older than a configured grace period) to a
	 * deletion directory to allow safe manual inspection.
	 */
	public void cleanOrphanResources() {
		logger.info("Starting orphan resource cleanup batch job...");

		String cachingFolder = resourceService.getRemoteCachingFolder();
		String deletionFolder = apiProperties.remoteCachingDeletionFolder();
		long gracePeriodMs = apiProperties.getResourceCleanupGracePeriodMs();
		List<Integer> allowedSuffixes = apiProperties.getAllowedImagesSizeSuffixes();

		File cacheDir = new File(cachingFolder);
		if (!cacheDir.exists() || !cacheDir.isDirectory()) {
			logger.warn("Cache directory does not exist or is not a directory: {}", cachingFolder);
			return;
		}

		// 1. Scan filesystem for all cached files
		Set<File> scannedFiles = new HashSet<>();
		collectCacheFiles(cacheDir, scannedFiles);
		logger.info("Scanned {} files in the cache directory.", scannedFiles.size());

		// 2. Stream all products to extract active resource cache keys
		Set<String> activeKeys = new HashSet<>();
		long activeProductCount = 0;
		try (Stream<Product> productStream = dataRepository.exportAll()) {
			Iterator<Product> iterator = productStream.iterator();
			while (iterator.hasNext()) {
				Product product = iterator.next();
				activeProductCount++;
				if (product.getResources() != null) {
					for (Resource r : product.getResources()) {
						if (r.getCacheKey() != null) {
							// Active original cache key
							activeKeys.add(r.getCacheKey());

							// If it's an image, precompute and add original and resized WebP cache keys
							if (r.getResourceType() == ResourceType.IMAGE) {
								if (r.path() != null) {
									activeKeys.add(IdHelper.generateResourceId(r.path()) + ".cache.webp");
								}
								if (allowedSuffixes != null) {
									for (Integer suffix : allowedSuffixes) {
										String pathWithSuffix = r.path(suffix);
										if (pathWithSuffix != null) {
											activeKeys.add(IdHelper.generateResourceId(pathWithSuffix) + ".cache.webp");
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error streaming products from repository during resource cleanup", e);
			return;
		}
		logger.info("Processed {} active products. Found {} active resource cache keys.", activeProductCount, activeKeys.size());

		// 3. Compare and move orphans to deletion folder
		long totalSpaceMovedBytes = 0;
		long movedCount = 0;
		long skippedGracePeriodCount = 0;
		long activePreservedCount = 0;
		long currentTime = System.currentTimeMillis();

		for (File file : scannedFiles) {
			String fileName = file.getName();
			if (activeKeys.contains(fileName)) {
				activePreservedCount++;
				continue;
			}

			// Check grace period
			if (currentTime - file.lastModified() <= gracePeriodMs) {
				skippedGracePeriodCount++;
				continue;
			}

			// Move orphan maintaining hierarchy
			String relativePath = file.getAbsolutePath().substring(cacheDir.getAbsolutePath().length());
			if (relativePath.startsWith(File.separator)) {
				relativePath = relativePath.substring(1);
			}

			File destFile = new File(deletionFolder, relativePath);
			File destParent = destFile.getParentFile();
			if (destParent != null && !destParent.exists()) {
				destParent.mkdirs();
			}

			long fileSize = file.length();
			try {
				java.nio.file.Files.move(file.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				totalSpaceMovedBytes += fileSize;
				movedCount++;
				if (movedCount % 100 == 0) {
					logger.info("Progression: Moved {} orphaned resource files so far ({})...", movedCount, FileUtils.byteCountToDisplaySize(totalSpaceMovedBytes));
				}
			} catch (Exception e) {
				logger.error("Failed to move orphan file {} to {}", file.getAbsolutePath(), destFile.getAbsolutePath(), e);
			}
		}

		logger.info("Resource cleanup completed successfully.");
		logger.info("Summary: Moved {} files to deletion folder, saving {}.", movedCount, FileUtils.byteCountToDisplaySize(totalSpaceMovedBytes));
		logger.info("Preserved {} active cache files. Skipped {} files within grace period.", activePreservedCount, skippedGracePeriodCount);
	}

	private void collectCacheFiles(File folder, Set<File> files) {
		File[] children = folder.listFiles();
		if (children == null) {
			return;
		}
		for (File child : children) {
			if (child.isDirectory()) {
				collectCacheFiles(child, files);
			} else if (child.isFile()) {
				files.add(child);
			}
		}
	}

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
		try (java.util.stream.Stream<Product> stream = dataRepository.getProductsMatchingVerticalId(vertical)) {
			allProducts = stream.collect(Collectors.toSet());
		}

		logger.info("Sanitisation of {} products for vertical {}", allProducts.size(), vertical.getId());

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
    @Scheduled(cron = "${feed.indexation.cron:19 13 23 * * ?}")
    public void fetchFeeds() {
        fetchFeeds(null);
    }

    /**
     * Fetches feeds from a single affiliation provider when requested, or all feeds otherwise.
     *
     * @param providerName optional affiliation provider name filter
     */
    public void fetchFeeds(String providerName) {
        logger.info("Initiating full feed fetching process.");
        Set<DataSourceProperties> datasources = feedService.getFeedsUrl(providerName);

        List<DataSourceProperties> datasourceList = new ArrayList<>(datasources);
        long seed = System.nanoTime();
        java.util.Collections.shuffle(datasourceList, new java.util.Random(seed));

        logger.info("Total feeds to fetch: {}", datasourceList.size());
        for (DataSourceProperties ds : datasourceList) {
            try {
                logger.info("Fetching feed: {}", ds);
                feedIndexingService.start(ds, ds.getDatasourceConfigName());
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
        fetchFeedsByUrl(url, null);
    }

    /**
     * Fetches feeds that match the specified feed URL and optional provider.
     *
     * @param url the feed URL to match
     * @param providerName optional affiliation provider name filter
     */
    public void fetchFeedsByUrl(String url, String providerName) {
        logger.info("Fetching feeds with URL: {}", url);
        Set<DataSourceProperties> datasources =  feedService.getFeedsUrl(providerName);
        logger.info("Found {} feeds for processing.", datasources.size());

        for (DataSourceProperties ds : datasources) {
            try {
                if (ds.getCsvDatasource().getDatasourceUrls().contains(url)) {
                    logger.info("Fetching feed: {}", ds);
                    feedIndexingService.start(ds, ds.getDatasourceConfigName());
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
        fetchFeedsByKey(feedKey, null);
    }

    /**
     * Fetches feeds that match the specified feed key and optional provider.
     *
     * @param feedKey the feed key to match
     * @param providerName optional affiliation provider name filter
     */
    public void fetchFeedsByKey(String feedKey, String providerName) {
        logger.info("Fetching feeds with key: {}", feedKey);
        Set<DataSourceProperties> datasources = matchingKey(feedKey, providerName);
        for (DataSourceProperties ds : datasources) {
            try {
                logger.info("Fetching feed {}: {}", ds.getDatasourceConfigName(), ds);
                feedIndexingService.start(ds, ds.getDatasourceConfigName());
            } catch (Exception e) {
                logger.error("Error fetching feed {}: ", ds.getDatasourceConfigName(), e);
            }
        }
    }

    /**
     * Fetches feeds that match the specified datasource/provider name.
     *
     * @param datasourceName the datasource/provider name to match
     */
    public void fetchFeedsByDatasourceName(String datasourceName)
    {
        fetchFeedsByDatasourceName(datasourceName, null);
    }

    /**
     * Fetches feeds that match the specified datasource/provider name and optional provider.
     *
     * @param datasourceName the datasource/provider name to match
     * @param providerName optional affiliation provider name filter
     */
    public void fetchFeedsByDatasourceName(String datasourceName, String providerName)
    {
        logger.info("Fetching feeds with datasource name: {}", datasourceName);
        Set<DataSourceProperties> datasources = feedService.getFeedsByDatasourceName(datasourceName, providerName);
        logger.info("Found {} feeds for datasource name matching.", datasources.size());
        for (DataSourceProperties ds : datasources) {
            try {
                logger.info("Fetching feed {}: {}", ds.getDatasourceConfigName(), ds);
                feedIndexingService.start(ds, ds.getDatasourceConfigName());
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
        return matchingKey(feedKey, null);
    }

    private Set<DataSourceProperties> matchingKey(String feedKey, String providerName) {
        String cleanedKey = IdHelper.azCharAndDigits(feedKey).toLowerCase();
        Set<DataSourceProperties> result = new HashSet<>();
        for (DataSourceProperties ds : feedService.getFeedsUrl(providerName)) {
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

		// Cleaning data
		cleanAiData();




		AtomicInteger counter = new AtomicInteger();
		try (java.util.stream.Stream<Product> stream = dataRepository.exportAll()) {
			stream.forEach(p -> {
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

	/**
	 * Clean AI data from all verticals
	 */
	public void cleanAiData() {
		for (VerticalConfig vertical : verticalsConfigService.getConfigsWithoutDefault()) {
			cleanAiData(vertical);
		}
	}

	/**
	 * Clean AI data from a specific vertical
	 * @param vertical
	 */
	public void cleanAiData(VerticalConfig vertical) {
		logger.info("Cleaning AI data for vertical {}", vertical);
		try (java.util.stream.Stream<Product> stream = dataRepository.getProductsMatchingVerticalId(vertical)) {
			stream.forEach(p -> {
				boolean changed = false;

			// 1. Reset/Delete aireview field
			if (p.getReviews() != null && !p.getReviews().isEmpty()) {
				p.getReviews().clear();
				changed = true;
			}

			// 2. Delete attribute sources having "ai" as datasourcename
			// Check Indexed Attributes
			Iterator<IndexedAttribute> itIndexed = p.getAttributes().getIndexed().values().iterator();
			while (itIndexed.hasNext()) {
				IndexedAttribute attr = itIndexed.next();
				boolean attrChanged = attr.getSource().removeIf(s -> s.getDataSourcename() != null && s.getDataSourcename().toLowerCase().contains("ai"));
				if (attrChanged) {
					if (attr.getSource().isEmpty()) {
						itIndexed.remove();
					} else {
						// Recompute value
						String best = attr.bestValue();
						attr.setValue(best);
						attr.setNumericValue(parseNumericOrNull(best));
					}
					changed = true;
				}
			}

			// Check All Attributes (ProductAttribute)
			Iterator<ProductAttribute> itAll = p.getAttributes().getAll().values().iterator();
			while (itAll.hasNext()) {
				ProductAttribute attr = itAll.next();
				boolean attrChanged = attr.getSource().removeIf(s -> s.getDataSourcename() != null && s.getDataSourcename().toLowerCase().contains("ai"));
				if (attrChanged) {
					if (attr.getSource().isEmpty()) {
						itAll.remove();
					} else {
						// Recompute value
						String best = attr.bestValue();
						attr.setValue(best);
					}
					changed = true;
				}
			}

			if (changed) {
				dataRepository.index(p);
			}
		});
		}
	}

	private Double parseNumericOrNull(String rawValue) {
		if (rawValue == null) {
			return null;
		}
		try {
			return Double.valueOf(rawValue.trim().replace(",", "."));
		} catch (NumberFormatException e) {
			return null;
		}
	}



}
