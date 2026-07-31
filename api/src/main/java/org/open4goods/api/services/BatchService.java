package org.open4goods.api.services;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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
	 * Depth of the hash shard hierarchy produced by {@link Resource#folderHashPrefix(String)},
	 * below which - and only below which - product resource files are stored.
	 */
	private static final int SHARD_DEPTH = 3;

	/**
	 * Shard folder name used by {@link Resource#folderHashPrefix(String)} for cache keys too
	 * short to be sharded on their three last characters.
	 */
	private static final String UNKNOWN_SHARD = "UNKNOWN";

	/**
	 * Runs the orphan resource cleanup in report-only mode.
	 *
	 * @return the classification report, without any file having been touched
	 */
	public ResourceCleanupReport cleanOrphanResources() {
		return cleanOrphanResources(ResourceCleanupMode.DRY_RUN);
	}

	/**
	 * Scans the product resource shards of the local cache directory, compares the files
	 * against every resource reachable from the product repository, and reports, moves or
	 * deletes the orphans depending on the requested mode.
	 * <p>
	 * Two invariants keep this safe to run against production:
	 * <ul>
	 * <li>The product enumeration is validated against the index count taken beforehand. A
	 * truncated or empty stream aborts the run instead of declaring the whole cache orphaned.</li>
	 * <li>Only files laid out as {@code <shard>/<shard>/<shard>/<cacheKey>} are considered.
	 * The cache root is shared with unrelated caches (remote files and Icecat archives at the
	 * root, {@code geocode/}, {@code batch-ia/}) which must never be swept here.</li>
	 * </ul>
	 * <p>
	 * Shard scoping is a filter on layout, not on ownership: anything written through
	 * {@code ResourceService.getCacheFile(hash)} lands in the same shards and is swept too. Today
	 * that means the GTIN barcodes of {@code GtinService}, which are not product resources and are
	 * regenerated on the next request, so their loss is a cache miss and nothing more.
	 *
	 * @param mode action to apply to the orphans
	 * @return a report of what was classified and what was actually processed
	 */
	public ResourceCleanupReport cleanOrphanResources(ResourceCleanupMode mode) {
		logger.info("Starting orphan resource cleanup batch job in {} mode...", mode);

		String cachingFolder = resourceService.getRemoteCachingFolder();
		long gracePeriodMs = apiProperties.getResourceCleanupGracePeriodMs();

		File cacheDir = new File(cachingFolder);
		if (!cacheDir.exists() || !cacheDir.isDirectory()) {
			logger.warn("Cache directory does not exist or is not a directory: {}", cachingFolder);
			return ResourceCleanupReport.aborted(mode, "cache directory not found: " + cachingFolder, 0, 0);
		}

		// 1. Take the reference product count BEFORE streaming, to detect a truncated stream
		long indexedProductCount;
		try {
			Long count = dataRepository.countMainIndex();
			indexedProductCount = count == null ? 0L : count;
		} catch (Exception e) {
			logger.error("Cannot count indexed products, aborting resource cleanup", e);
			return ResourceCleanupReport.aborted(mode, "product count failed: " + e.getMessage(), 0, 0);
		}
		if (indexedProductCount == 0) {
			logger.error("Product index reports 0 documents, aborting resource cleanup");
			return ResourceCleanupReport.aborted(mode, "product index is empty", 0, 0);
		}

		// 2. Stream all products to extract active resource cache keys
		Set<String> activeKeys = new HashSet<>();
		long streamedProductCount;
		try {
			streamedProductCount = collectActiveKeys(activeKeys);
		} catch (Exception e) {
			logger.error("Error streaming products from repository during resource cleanup", e);
			return ResourceCleanupReport.aborted(mode, "product stream failed: " + e.getMessage(), indexedProductCount, 0);
		}

		// 3. Safety floor : a silently truncated stream must never be read as "nothing is referenced"
		long floor = (long) Math.ceil(indexedProductCount * apiProperties.getResourceCleanupMinProductRatio());
		if (streamedProductCount < floor) {
			String reason = "streamed " + streamedProductCount + " products, below the safety floor of " + floor + " ("
					+ indexedProductCount + " indexed)";
			logger.error("Aborting resource cleanup, no file touched : {}", reason);
			return ResourceCleanupReport.aborted(mode, reason, indexedProductCount, streamedProductCount);
		}
		logger.info("Processed {} active products (of {} indexed). Found {} active resource cache keys.",
				streamedProductCount, indexedProductCount, activeKeys.size());

		// 4. Walk the product shards and act on the orphans, one file at a time
		OrphanSweep sweep = new OrphanSweep(mode, cacheDir, activeKeys, gracePeriodMs, System.currentTimeMillis());
		sweepShards(cacheDir, sweep);

		ResourceCleanupReport report = sweep.toReport(indexedProductCount, streamedProductCount, activeKeys.size());
		logger.info("Resource cleanup completed. Scanned {} files, preserved {} active, skipped {} within grace period.",
				report.scannedFileCount(), report.preservedCount(), report.gracePeriodCount());
		logger.info("Identified {} orphans totalling {} ; {} processed in {} mode, {} failures.", report.orphanCount(),
				report.reclaimableSize(), report.processedCount(), mode, report.failureCount());
		if (report.skippedOutOfScopeCount() > 0) {
			logger.info("Ignored {} files living outside the product resource shards (foreign caches).",
					report.skippedOutOfScopeCount());
		}
		return report;
	}

	/**
	 * Streams every product and registers all cache keys reachable from its resources.
	 *
	 * @param activeKeys set to fill with the reachable cache keys
	 * @return the number of products actually streamed
	 */
	private long collectActiveKeys(Set<String> activeKeys) {
		List<Integer> allowedSuffixes = apiProperties.getAllowedImagesSizeSuffixes();
		long streamedProductCount = 0;

		try (Stream<Product> productStream = dataRepository.exportAll()) {
			Iterator<Product> iterator = productStream.iterator();
			while (iterator.hasNext()) {
				Product product = iterator.next();
				streamedProductCount++;
				if (product.getResources() == null) {
					continue;
				}
				for (Resource r : product.getResources()) {
					if (r.getCacheKey() == null) {
						continue;
					}
					// Original file, stored under its raw cache key
					activeKeys.add(r.getCacheKey());

					// Images are also served as WebP variants generated by the resize interceptor,
					// which keys them on the request URI, ie on Resource.path()
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
		return streamedProductCount;
	}

	/**
	 * Walks the cache directory, descending only into the hash shard hierarchy holding
	 * product resources, and submits each file found there to the sweep.
	 */
	private void sweepShards(File cacheDir, OrphanSweep sweep) {
		File[] topLevel = cacheDir.listFiles();
		if (topLevel == null) {
			return;
		}
		for (File child : topLevel) {
			if (child.isDirectory() && isShardFolder(child.getName())) {
				sweepShards(child, sweep, 1);
			} else {
				// Foreign caches : favicons and remote files at the root, geocode/, batch-ia/, Icecat archives
				sweep.outOfScope();
			}
		}
	}

	private void sweepShards(File folder, OrphanSweep sweep, int depth) {
		File[] children = folder.listFiles();
		if (children == null) {
			return;
		}
		for (File child : children) {
			if (child.isDirectory()) {
				if (depth < SHARD_DEPTH && isShardFolder(child.getName())) {
					sweepShards(child, sweep, depth + 1);
				} else {
					sweep.outOfScope();
					continue;
				}
				// Drop shards emptied by a DELETE run, list() returning an empty array
				String[] remaining = child.list();
				if (remaining != null && remaining.length == 0) {
					child.delete();
				}
			} else if (child.isFile()) {
				if (depth == SHARD_DEPTH) {
					sweep.accept(child);
				} else {
					sweep.outOfScope();
				}
			}
		}
	}

	/**
	 * @return true when the folder name is one of the shard names emitted by
	 *         {@link Resource#folderHashPrefix(String)}
	 */
	private static boolean isShardFolder(String name) {
		return name.length() == 1 || UNKNOWN_SHARD.equals(name);
	}

	/**
	 * Classifies cache files against the active keys and applies the requested mode,
	 * accumulating the counters of a single run. Files are handled one at a time so that
	 * a multi million file cache never has to be materialised in memory.
	 */
	private final class OrphanSweep {

		private final ResourceCleanupMode mode;
		private final File cacheDir;
		private final Set<String> activeKeys;
		private final long gracePeriodMs;
		private final long startTime;

		private long scanned;
		private long outOfScope;
		private long preserved;
		private long withinGracePeriod;
		private long orphans;
		private long orphanBytes;
		private long processed;
		private long failures;

		private OrphanSweep(ResourceCleanupMode mode, File cacheDir, Set<String> activeKeys, long gracePeriodMs,
				long startTime) {
			this.mode = mode;
			this.cacheDir = cacheDir;
			this.activeKeys = activeKeys;
			this.gracePeriodMs = gracePeriodMs;
			this.startTime = startTime;
		}

		private void outOfScope() {
			outOfScope++;
		}

		private void accept(File file) {
			scanned++;

			if (activeKeys.contains(file.getName())) {
				preserved++;
				return;
			}
			if (startTime - file.lastModified() <= gracePeriodMs) {
				withinGracePeriod++;
				return;
			}

			long fileSize = file.length();
			orphans++;
			orphanBytes += fileSize;

			if (mode == ResourceCleanupMode.DRY_RUN) {
				logger.debug("Would reclaim orphan resource {} ({} bytes)", file.getAbsolutePath(), fileSize);
				return;
			}

			try {
				if (mode == ResourceCleanupMode.DELETE) {
					java.nio.file.Files.delete(file.toPath());
				} else {
					move(file);
				}
				processed++;
				if (processed % 1000 == 0) {
					logger.info("Progression: {} orphaned resource files processed so far ({})...", processed,
							FileUtils.byteCountToDisplaySize(orphanBytes));
				}
			} catch (Exception e) {
				failures++;
				logger.error("Failed to {} orphan file {}", mode, file.getAbsolutePath(), e);
			}
		}

		private void move(File file) throws IOException {
			String relativePath = file.getAbsolutePath().substring(cacheDir.getAbsolutePath().length());
			if (relativePath.startsWith(File.separator)) {
				relativePath = relativePath.substring(1);
			}

			File destFile = new File(apiProperties.remoteCachingDeletionFolder(), relativePath);
			File destParent = destFile.getParentFile();
			if (destParent != null && !destParent.exists()) {
				destParent.mkdirs();
			}
			java.nio.file.Files.move(file.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}

		private ResourceCleanupReport toReport(long indexedProductCount, long streamedProductCount, long activeKeyCount) {
			return new ResourceCleanupReport(mode, false, null, indexedProductCount, streamedProductCount, activeKeyCount,
					scanned, outOfScope, preserved, withinGracePeriod, orphans, orphanBytes, processed, failures);
		}
	}

	/**
	 * Deletes for real the files parked in the deletion folder by
	 * {@link ResourceCleanupMode#MOVE} once they are older than the configured retention.
	 * Moving between two directories of the same filesystem frees no space, so this is what
	 * actually reclaims the disk.
	 *
	 * @return the number of bytes reclaimed
	 */
	public long purgeDeletionFolder() {
		File deletionDir = new File(apiProperties.remoteCachingDeletionFolder());
		if (!deletionDir.exists() || !deletionDir.isDirectory()) {
			logger.info("Deletion folder {} does not exist, nothing to purge", deletionDir.getAbsolutePath());
			return 0;
		}

		long cutoff = System.currentTimeMillis() - apiProperties.getResourceDeletionFolderRetentionMs();
		AtomicLong reclaimed = new AtomicLong();
		AtomicInteger deleted = new AtomicInteger();

		purgeDeletionFolder(deletionDir, cutoff, reclaimed, deleted);

		logger.info("Purged {} files from the deletion folder, reclaiming {}.", deleted.get(),
				FileUtils.byteCountToDisplaySize(reclaimed.get()));
		return reclaimed.get();
	}

	private void purgeDeletionFolder(File folder, long cutoff, AtomicLong reclaimed, AtomicInteger deleted) {
		File[] children = folder.listFiles();
		if (children == null) {
			return;
		}
		for (File child : children) {
			if (child.isDirectory()) {
				purgeDeletionFolder(child, cutoff, reclaimed, deleted);
				// Drop the directory once emptied, listFiles() returning an empty array
				String[] remaining = child.list();
				if (remaining != null && remaining.length == 0) {
					child.delete();
				}
			} else if (child.isFile() && child.lastModified() < cutoff) {
				long size = child.length();
				if (child.delete()) {
					reclaimed.addAndGet(size);
					deleted.incrementAndGet();
				} else {
					logger.warn("Could not purge parked file {}", child.getAbsolutePath());
				}
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

			// Delete attribute sources having "ai" as datasourcename
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
