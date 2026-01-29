package org.open4goods.services.productrepository.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.BarcodeType;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductPartialUpdateHolder;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.SubsetCriteriaOperator;
import org.open4goods.services.productrepository.config.IndexationConfig;
import org.open4goods.services.productrepository.workers.FullProductIndexationWorker;
import org.open4goods.services.productrepository.workers.PartialProductIndexationWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.MultiGetItem;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.ExtendedStatsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.mapping.FieldType;
import co.elastic.clients.elasticsearch._types.ScriptSortType;


/**
 * The Elastic Data Access Object for products TODO : Could maintain the elastic
 * buffer queue for ingestion here ?
 *
 * @author goulven
 *
 */
public class ProductRepository {

        private static final Logger logger = LoggerFactory.getLogger(ProductRepository.class);

        public static final String MAIN_INDEX_NAME = Product.DEFAULT_REPO;

        /**
         * Default page size used when streaming large exports.
         */
        private static final int EXPORT_STREAM_PAGE_SIZE = 5000;

        /**
         * Lazily created {@link PageRequest} that avoids instantiating a new object for each export.
         */
        private static final PageRequest EXPORT_STREAM_PAGE = PageRequest.of(0, EXPORT_STREAM_PAGE_SIZE);

        /**
         * Fields required when building the public OpenData CSVs. Fetching only these fields keeps
         * the Elasticsearch payload lean, which considerably lowers the CPU pressure of bulk exports.
         */
        private static final String[] OPEN_DATA_EXPORT_FIELDS = {
                        "id",
                        "attributes",
                        "gtinInfos",
                        "offersCount",
                        "price",
                        "datasourceCategories",
                        "offerNames",
                        "lastChange"
        };


	// The file queue implementation for Full products (no partial updates)
	private BlockingQueue<Product> fullProductQueue;

	// The file queue implementation for Full products (no partial updates)
	private BlockingQueue<ProductPartialUpdateHolder> partialProductQueue;



	/**
	 * !!!MAJOR CONST !!! Duration in ms where a price is considered to be valid. Only data with a
	 * price greater than this one will be returned to the user. Also defines the caching TTL of redis
	 **/
	// TODO(p1, conf) : Major constant, from conf
	public final static long VALID_UNTIL_DURATION = 1000 * 3600 * 24 * 2;

	private static final int MAX_TITLE_ITEMS_TO_FETCH = 5;

	public static IndexCoordinates CURRENT_INDEX = IndexCoordinates.of(MAIN_INDEX_NAME);

	private @Autowired ElasticsearchOperations elasticsearchOperations;


//	private @Autowired RedisProductRepository redisRepository;

//	private @Autowired RedisOperations<String, Product> redisRepo;

	public ProductRepository() {


	}

//	private ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	//TODO(p3,perf) : Virtual threads, but ko with visualVM profiling
	public ProductRepository(IndexationConfig indexationConfig) {

		this.fullProductQueue = new LinkedBlockingQueue<>(indexationConfig.getProductsQueueMaxSize());
		this.partialProductQueue = new LinkedBlockingQueue<>(indexationConfig.getPartialProductsQueueMaxSize());

		for (int i = 0; i < indexationConfig.getProductWorkers(); i++) {
			new Thread((new FullProductIndexationWorker(this, indexationConfig.getProductsbulkPageSize(), indexationConfig.getPauseDuration(),"full-products-worker-"+i))).start();
		}

		for (int i = 0; i < indexationConfig.getPartialProductWorkers(); i++) {
			new Thread((new PartialProductIndexationWorker(this, indexationConfig.getPartialProductsbulkPageSize(), indexationConfig.getPauseDuration(),"partial-products-worker-"+i))).start();
		}

	}



	public Stream<Product> getProductsMatchingVerticalId(VerticalConfig v) {
		Criteria c = new Criteria("vertical").is(v.getId());

		final NativeQuery initialQuery = new NativeQueryBuilder().withQuery(new CriteriaQuery(c)).build();

		return elasticsearchOperations.searchForStream(initialQuery, Product.class, CURRENT_INDEX).stream()
				.map(SearchHit::getContent);

	}

	/**
	 * Export all aggregated data
	 *
	 * @return
	 */
        public Stream<Product> exportAll() {
            Query query = Query.findAll();
            query.setPageable(EXPORT_STREAM_PAGE); // Fetch larger batches
            // Stream sequentially to avoid parallel overhead
            return elasticsearchOperations
                    .searchForStream(query, Product.class, CURRENT_INDEX)
                    .stream()
                    .map(SearchHit::getContent);
        }


    public SearchHits<Product> get(Pageable page) {
        Query query = Query.findAll();
        query.setPageable(page);
        try {
			return elasticsearchOperations.search(query, Product.class, CURRENT_INDEX);
		} catch (Exception e) {
			elasticLog(e);
			// TODO : Should throw
			return null;
		}
    }

	/**
	 * Retrieves only the mappedCategories for items having the minimal attributes set
	 * @return
	 */
	public Stream<Product> exportForCategoriesMapping(Set<String> mustExistsfields, Integer maxResults) {
	    Criteria c = new Criteria("datasourceCategories").exists();
	    c = c.and(getRecentPriceQuery());

	    for (String f : mustExistsfields) {
	    	c = c.and(new Criteria(f).exists());
	    }

	    NativeQueryBuilder initialQueryBuilder = new NativeQueryBuilder()
	            .withQuery(new CriteriaQuery(c))
	            .withSourceFilter(new FetchSourceFilter(true,new String[]{"categoriesByDatasources"}, null));

	    if (null != maxResults) {
//            initialQueryBuilder = initialQueryBuilder.withMaxResults(maxResults);
	    }

	    NativeQuery initialQuery = initialQueryBuilder.build();

	    initialQuery.setPageable(PageRequest.of(0, 1000));

	    return elasticsearchOperations.searchForStream(initialQuery, Product.class, CURRENT_INDEX).stream()
	            .map(SearchHit::getContent);
	}



	public Stream<Product> exportAll(String vertical) {

		Criteria c = new Criteria("vertical").is(vertical);

		final NativeQuery initialQuery = new NativeQueryBuilder()
				.withQuery(new CriteriaQuery(c)).build();

		return elasticsearchOperations.searchForStream(initialQuery, Product.class, CURRENT_INDEX).stream()
				.map(SearchHit::getContent);
	}

	/**
	 * Export all aggregated data, corresponding to the given Barcodes
	 *
	 * @return
	 */


        public Stream<Product> exportAll( String[] includeFields) {

                NativeQueryBuilder queryBuilder = new NativeQueryBuilder().withQuery(Query.findAll());
                if (includeFields != null && includeFields.length > 0) {
                        queryBuilder = queryBuilder.withSourceFilter(new FetchSourceFilter(true, includeFields, null));
                }

                NativeQuery query = queryBuilder
                		.withPageable(EXPORT_STREAM_PAGE)
                		.build();

                return elasticsearchOperations.searchForStream(query, Product.class, CURRENT_INDEX).stream()
                                .map(SearchHit::getContent);
        }


	public Stream<Product> searchInValidPrices(String query, final String indexName, int from, int to) {

		Criteria c = new Criteria().expression(query).and(getRecentPriceQuery());

		final NativeQuery initialQuery = new NativeQueryBuilder().withQuery(new CriteriaQuery(c))
				.withPageable(PageRequest.of(from, to)).build();

		return elasticsearchOperations.search(initialQuery, Product.class, CURRENT_INDEX).stream()
				.map(SearchHit::getContent);

	}



	// TODO(P2,design) : in a stat service

	/**
	 * Return the scores coverage stats for a vertical
	 * @param vConf
	 * @return
	 */
	public Map<String,Long> scoresCoverage(VerticalConfig vConf) {

                Map<String, Long> ret = new HashMap<>();

                if (vConf.getAvailableImpactScoreCriterias() == null) {
                        logger.debug("No available impact score criteria configured for vertical {}", vConf.getId());
                        return ret;
                }

                vConf.getAvailableImpactScoreCriterias().forEach(criteriaKey -> {

                        Long count = countMainIndexHavingScore(criteriaKey,vConf.getId());

                        // TODO(p2, conf) : threshold from conf
                        if (count > 10) {
                                ret.put(criteriaKey ,  count);
                        } else {
                                logger.info("Excluded from score mapping : {}", criteriaKey );
                        }
                });


		return ret;
	}

	/**
	 * Export all aggregateddatas for a vertical
	 *
	 * @param vertical
	 * @param max
	 * @param indexName
	 * @return
	 */
	public Stream<Product> exportVerticalWithValidDate(VerticalConfig vertical, boolean withExcluded) {




		Criteria c = getRecentPriceQuery()
				.and( new Criteria("vertical").is(vertical.getId()))

				//				.or(new Criteria("datasourceCategories").in(vertical.getMatchingCategories())
				;

		if (!withExcluded) {
            c = c.and(new Criteria("excluded").is(false));
        }

		final NativeQuery initialQuery = new NativeQueryBuilder()
				.withQuery(new CriteriaQuery(c)).build();
		return elasticsearchOperations.searchForStream(initialQuery, Product.class, CURRENT_INDEX).stream()
				.map(SearchHit::getContent);
	}


	/**
	 * Export all products in a vertical having at least the provided minOfferscount
	 * @param vertical
	 * @param minOfferscount
	 * @return
	 */
	public Stream<Product> exportVerticalWithOffersCountGreater(VerticalConfig vertical, Integer minOfferscount) {


		Criteria c = getRecentPriceQuery()
				.and( new Criteria("vertical").is(vertical.getId()))
				.and(new Criteria("offersCount").greaterThanEqual(minOfferscount) )

				//				.or(new Criteria("datasourceCategories").in(vertical.getMatchingCategories())
				;


		final NativeQuery initialQuery = new NativeQueryBuilder()
				.withQuery(new CriteriaQuery(c)).build();
		return elasticsearchOperations.searchForStream(initialQuery, Product.class, CURRENT_INDEX).stream()
				.map(SearchHit::getContent);
	}




	/**
	 * Export all aggregateddatas for a vertical, ordered by ecoscore descending
	 *
	 * @param vertical
	 * @param max
	 * @param max
	 * @param withExcluded
	 * @param indexName
	 * @return
	 */
	public Stream<Product> exportAllVerticalizedProductsWithGenAiSinceEpoch(Long epoch) {

		Criteria c = new Criteria("vertical").exists()
				.and(getSinceDateQuery(epoch))
				.and(new Criteria("aiDescriptions").exists())
				;
		NativeQueryBuilder initialQueryBuilder = new NativeQueryBuilder().withQuery(new CriteriaQuery(c));

                initialQueryBuilder =  initialQueryBuilder.withSort(Sort.by(org.springframework.data.domain.Sort.Order.desc("scores.ECOSCORE.relativ.value")));

		NativeQuery initialQuery = initialQueryBuilder.build();

		return elasticsearchOperations.searchForStream(initialQuery, Product.class, CURRENT_INDEX).stream().map(SearchHit::getContent);
	}






	/**
	 * Export all aggregateddatas for a vertical, ordered by ecoscore descending
	 *
	 * @param vertical
	 * @param max
	 * @param max
	 * @param withExcluded
	 * @param indexName
	 * @return
	 */

	public Stream<Product> exportVerticalWithValidDateOrderByEcoscore(String vertical, Integer max, boolean withExcluded) {

	    Criteria criteria = new Criteria("vertical").is(vertical)
	        .and(getRecentPriceQuery());

	    if (!withExcluded) {
	        criteria = criteria.and(new Criteria("excluded").is(false));
	    }

            // Ensure scores.ECOSCORE.relativ.value is present
            criteria = criteria.and(new Criteria("scores.ECOSCORE.value").exists());

	    // Build sort with unmapped_type to avoid shard-level mapping issues
	    SortOptions ecoscoreSort = new SortOptions.Builder()
	        .field(new FieldSort.Builder()
                .field("scores.ECOSCORE.value")
	            .order(SortOrder.Desc)
	            .unmappedType(FieldType.Float)
	            .missing("_last")
	            .build())
	        .build();

	    NativeQueryBuilder queryBuilder = new NativeQueryBuilder()
	        .withQuery(new CriteriaQuery(criteria))
	        .withSort(ecoscoreSort);

	    if (max != null) {
	        queryBuilder = queryBuilder.withMaxResults(max);
	    }

	    NativeQuery query = queryBuilder.build();

	    try {
	        return elasticsearchOperations
	            .searchForStream(query, Product.class, CURRENT_INDEX)
	            .stream()
	            .map(SearchHit::getContent);
	    } catch (Exception e) {
	        elasticLog(e);
			throw e;
	    }
	}

	private void elasticLog(Exception e)  {
		if (e instanceof UncategorizedElasticsearchException) {
		    Throwable cause = e.getCause();
		    if (cause instanceof ElasticsearchException ee) {
		        logger.error("Elasticsearch error: " + ee.response());
		    } else {
		    	logger.error("Error : ",e );
		    }
		}
	}


	public Stream<Product> getAllHavingVertical() {
		Criteria c = new Criteria("vertical").exists()
				;


		NativeQueryBuilder initialQueryBuilder = new NativeQueryBuilder().withQuery(new CriteriaQuery(c));

                initialQueryBuilder =  initialQueryBuilder.withSort(Sort.by(org.springframework.data.domain.Sort.Order.desc("scores.ECOSCORE.relativ.value")));

		NativeQuery initialQuery = initialQueryBuilder.build();

		return elasticsearchOperations.searchForStream(initialQuery, Product.class, CURRENT_INDEX).stream().map(SearchHit::getContent);

	}



	/**
	 * Export all aggregateddatas for a vertical, ordered by ecoscore descending
	 *
	 * @param vertical
	 * @param max
	 * @param max
	 * @param indexName
	 * @return
	 */

	public Stream<Product> exportVerticalWithValidDateOrderByEcoscore(String vertical, boolean withExcluded) {
		return exportVerticalWithValidDateOrderByEcoscore(vertical, null, withExcluded);
	}

    /**
     * Export all aggregated data for a vertical, ordered by impact score descending.
     *
     * @param vertical the vertical identifier
     * @param max maximum number of products to fetch
     * @param withExcluded whether to include excluded products
     * @return stream of products ordered by impact score
     */
    public Stream<Product> exportVerticalWithValidDateOrderByImpactScore(String vertical, Integer max, boolean withExcluded)
    {
        Criteria criteria = new Criteria("vertical").is(vertical)
                .and(getRecentPriceQuery());

        if (!withExcluded) {
            criteria = criteria.and(new Criteria("excluded").is(false));
        }

        criteria = criteria.and(new Criteria("scores.IMPACTSCORE.value").exists());

        SortOptions impactScoreSort = new SortOptions.Builder()
                .field(new FieldSort.Builder()
                        .field("scores.IMPACTSCORE.value")
                        .order(SortOrder.Desc)
                        .unmappedType(FieldType.Float)
                        .missing("_last")
                        .build())
                .build();

        NativeQueryBuilder queryBuilder = new NativeQueryBuilder()
                .withQuery(new CriteriaQuery(criteria))
                .withSort(impactScoreSort);

        if (max != null) {
            queryBuilder = queryBuilder.withMaxResults(max);
        }

        NativeQuery query = queryBuilder.build();

        try {
            return elasticsearchOperations
                    .searchForStream(query, Product.class, CURRENT_INDEX)
                    .stream()
                    .map(SearchHit::getContent);
        } catch (Exception e) {
            elasticLog(e);
            throw e;
        }
    }

    /**
     * Export all aggregated data for a vertical, ordered by impact score descending.
     *
     * @param vertical the vertical identifier
     * @param withExcluded whether to include excluded products
     * @return stream of products ordered by impact score
     */
    public Stream<Product> exportVerticalWithValidDateOrderByImpactScore(String vertical, boolean withExcluded)
    {
        return exportVerticalWithValidDateOrderByImpactScore(vertical, null, withExcluded);
    }

    /**
     * Export all aggregated data for a vertical that are missing an AI review, ordered by impact score descending.
     *
     * @param vertical the vertical identifier
     * @param locale the locale to check for missing review (e.g. "fr")
     * @param max maximum number of products to fetch
     * @param withExcluded whether to include excluded products
     * @return stream of products ordered by impact score
     */
    public Stream<Product> exportVerticalWithValidDateAndMissingReviewOrderByImpactScore(String vertical, String locale, Integer max, boolean withExcluded, boolean sortOnImpactScore)
    {
        Criteria criteria = new Criteria("vertical").is(vertical)
                .and(getRecentPriceQuery());

        if (!withExcluded) {
            criteria = criteria.and(new Criteria("excluded").is(false));
        }

        if (sortOnImpactScore) {
        	criteria = criteria.and(new Criteria("scores.IMPACTSCORE.value").exists());
        }

        // Filter products that DON'T have a review in the specified locale
    //    criteria = criteria.and(new Criteria("reviews." + locale + ".review").exists().not());

        SortOptions impactScoreSort = new SortOptions.Builder()
                .field(new FieldSort.Builder()
                        .field("scores.IMPACTSCORE.value")
                        .order(SortOrder.Desc)
                        .unmappedType(FieldType.Float)
                        .missing("_last")
                        .build())
                .build();

        NativeQueryBuilder queryBuilder = new NativeQueryBuilder()
                .withQuery(new CriteriaQuery(criteria))
                .withSort(impactScoreSort);

        if (max != null) {
            queryBuilder = queryBuilder.withMaxResults(max);
        }

        NativeQuery query = queryBuilder.build();

        try {
            return elasticsearchOperations
                    .searchForStream(query, Product.class, CURRENT_INDEX)
                    .stream()
                    .map(SearchHit::getContent);
        } catch (Exception e) {
            elasticLog(e);
            throw e;
        }
    }



	public SearchHits<Product> search(Query query, final String indexName) {
		return elasticsearchOperations.search(query, Product.class, IndexCoordinates.of(indexName));

	}

    /**
     * Get random products for a vertical
     * @param vertical
     * @param limit
     * @return
     */
    public List<Product> getRandomProducts(String vertical, int limit) {

        Criteria c = new Criteria("vertical").is(vertical)
                .and(new Criteria("scores.ECOSCORE.value").exists());

        // Script sort for random
        SortOptions sortOptions = new SortOptions.Builder()
                .script(s -> s
                        .script(sc -> sc
                                .source("Math.random()")
                        )
                        .type(ScriptSortType.Number)
                        .order(SortOrder.Asc)
                )
                .build();

        NativeQuery query = new NativeQueryBuilder()
                .withQuery(new CriteriaQuery(c))
                .withSort(sortOptions)
                .withMaxResults(limit)
                .build();

        return elasticsearchOperations.search(query, Product.class, CURRENT_INDEX)
                .stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    /**
     * Get random products globally with minimum offers count and optional vertical filter
     * @param limit max number of products
     * @param minOffersCount minimum number of offers
     * @param verticalId optional vertical filter
     * @return list of random products
     */
    public List<Product> getRandomProducts(int limit, int minOffersCount, String verticalId) {
        Criteria c = new Criteria("offersCount").greaterThanEqual(minOffersCount)
                .and(new Criteria("excluded").is(false))
                .and(getRecentPriceQuery());

        if (verticalId != null && !verticalId.isBlank()) {
            c = c.and(new Criteria("vertical").is(verticalId));
        }

        // Script sort for random
        SortOptions sortOptions = new SortOptions.Builder()
                .script(s -> s
                        .script(sc -> sc
                                .source("Math.random()")
                        )
                        .type(ScriptSortType.Number)
                        .order(SortOrder.Asc)
                )
                .build();

        NativeQuery query = new NativeQueryBuilder()
                .withQuery(new CriteriaQuery(c))
                .withSort(sortOptions)
                .withMaxResults(limit)
                .build();

        return elasticsearchOperations.search(query, Product.class, CURRENT_INDEX)
                .stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    /**
     * Get random products globally with minimum offers count
     * @param limit max number of products
     * @param minOffersCount minimum number of offers
     * @return list of random products
     */
    public List<Product> getRandomProducts(int limit, int minOffersCount) {
        return getRandomProducts(limit, minOffersCount, null);
    }




//	/**
//	 * Index an Product
//	 *
//	 * @param p
//	 */
//	public void index(final Product p, final String indexName) {
//
//		logger.info("Indexing single product : {} in index {}", p.gtin(), indexName);
//
////		executor.submit(() -> {
//			elasticsearchTemplate.save(p, IndexCoordinates.of(indexName));
////		});
//
//		saveToRedis(p);
//
//	}

	/**
	 * Index an Product
	 *
	 * @param p
	 */
	public void index(final Product p) {

		logger.info("Queuing single product : {}", p.gtin());

		try {
			fullProductQueue.put(p);
		} catch (Exception e) {
			logger.error("Cannot enqueue product {}",p,e);
		}

	}

//	/**
//	 * Bulk Index multiple Product
//	 *
//	 * @param p
//	 */
//	public void index(Collection<Product> data, final String indexName) {
//
//		logger.info("Queuing {} products in index {}", data.size(), indexName);
//
////		executor.submit(() -> {
//			elasticsearchTemplate.save(data, IndexCoordinates.of(indexName));
////		});
//
////		executor.submit(() -> {
//			redisRepo.opsForValue().multiSet(data.stream().collect(Collectors.toMap(Product::gtin, Function.identity())));
////		});
//
//	}

	/**
	 * Bulk Index multiple Product
	 *
	 * @param p
	 */
	public void addToFullindexationQueue(Collection<Product> data) {

		logger.info("Queuing {} products", data.size());

		data.forEach(e -> {

			try {
				fullProductQueue.put(e) ;
			} catch (Exception e1) {
				logger.error("!!!! exception, cannot enqueue product {}",e);
			}

		});
	}

	public void addToPartialIndexationQueue(Collection<ProductPartialUpdateHolder> data) {

		logger.info("Queuing {} products", data.size());

		data.forEach(e -> {

			try {
				partialProductQueue.put(e) ;
			} catch (Exception e1) {
				logger.error("!!!! exception, cannot enqueue product {}",e);
			}

		});
	}

	public void store(Collection<Product> data) {
	    logger.info("Indexing {} products", data.size());

	    List<IndexQuery> indexQueries = data.stream()
	        .map(p -> new IndexQueryBuilder()
	            .withId(String.valueOf(p.getId()))
	            .withObject(p)
	            .build())
	        .collect(Collectors.toList());

	    elasticsearchOperations.bulkIndex(indexQueries, CURRENT_INDEX);
	}



	public void forceIndex(Product data) {
		logger.info("Indexing  product {}", data.gtin());

//		executor.submit(() -> {
			elasticsearchOperations.save(data, CURRENT_INDEX);
//		});

//		executor.submit(() -> {
//			redisRepository.save(data);
//		});
	}



	/**
	 * Return an aggregated data by it's ID
	 *
	 * @param productId
	 * @param indexName
	 * @return
	 * @throws ResourceNotFoundException
	 */
//	@Cacheable(cacheNames = CacheConstants.ONE_MINUTE_LOCAL_CACHE_NAME)
	public Product getById(final Long productId) throws ResourceNotFoundException {

		logger.info("Getting product  {}", productId);
		// Getting from redis



//		Product result = redisRepo.opsForValue().get(productId);
		Product result = null;
//		try {
//			result = redisRepository.findById(productId).orElseThrow(ResourceNotFoundException::new);
//		} catch (ResourceNotFoundException e) {
//
//			result = null;
//
//		} catch (Exception e) {
//			logger.error("Error getting product {} from redis", productId, e);
//			result = null;
//		}

		// Fail, getting from elastic
		logger.info("Cache miss, getting product {} from elastic", productId);
		result = elasticsearchOperations.get(String.valueOf(productId), Product.class);

		if (null == result) {
			throw new ResourceNotFoundException("Product '" + productId + "' does not exists");
		}

		// found, adding it in redis cache
		saveToRedis(result);


		return result;

	}

	/**
	 * Get multiple data from ids
	 * @param title
	 * @return
	 */
	public List<Product> getByTitle(String title) {
		// Setting the query

		return getByTitle(title, MAX_TITLE_ITEMS_TO_FETCH);

	}

	/**
	 * Get multiple data from ids
	 * @param title
	 * @return
	 */
	public List<Product> getByTitle(String title, int maxItems) {
		// Setting the query

		List<String> words = List.of(title.split(" "));
		NativeQueryBuilder esQuery = new NativeQueryBuilder().withQuery(new CriteriaQuery(new Criteria("offerNames").matchesAll(words) ));
		SearchHits<Product> results = search(esQuery.withPageable(PageRequest.of(0, maxItems)).build(),ProductRepository.MAIN_INDEX_NAME);
		return results.stream().map(SearchHit::getContent).collect(Collectors.toList());

	}


	/**
	 * Get multiple data from ids
	 * @param ids
	 * @return
	 * @throws ResourceNotFoundException
	 */
	//TODO(P1,perf) : Heavy cache
	public Map<String, Product> multiGetById( final Collection<Long> ids)
			throws ResourceNotFoundException {

		logger.info("Getting {} products from default index",ids.size());
		Map<String, Product> ret = new HashMap<String, Product>();


		// Getting from redis
//		Iterable<Product> redisResults = redisRepository.findAllById(ids);
//		redisResults.forEach(e -> {
//			if (null != e) {
//				ret.put(e.gtin(), e);
//			}
//		});
//
		// Getting the one we don't have in redis from elastic
                Set<String> missingIds = computeMissingIds(ids, ret);
		logger.info("returned hits : {}, missing : {}",ret.size(), missingIds.size());


                if (missingIds.size() != 0) {


			NativeQuery query = new NativeQueryBuilder().withIds(missingIds).build();

			elasticsearchOperations.multiGet(query, Product.class,CURRENT_INDEX )
			.stream().map(MultiGetItem::getItem)
			.filter(Objects::nonNull)
			.forEach(e -> ret.put(e.gtin(), e));


			// Filtrer et collecter les produits à partir d'une liste en utilisant leur GTIN comme clé dans une map
			Set<Product> redisItems = ret.values().stream()
			    // Filtrer les éléments non nuls
			    .filter(Objects::nonNull)
			    // Filtrer les éléments dont le GTIN est présent dans la liste des IDs manquants
			    .filter(e -> missingIds.contains(e.gtin()))
			    // Collecter les produits dans une map avec le GTIN comme clé
			    .collect(Collectors.toSet());

			logger.info("Saving {} products in redis",redisItems.size());
//			executor.submit(() -> {

//			redisRepository.saveAll(redisItems);
//			});
		}

		return ret;
	}


	@Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public Long countMainIndex() {
		return elasticsearchOperations.count(Query.findAll(), CURRENT_INDEX);
	}

        @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
        public Long countMainIndexHavingRecentPrices() {
                CriteriaQuery query = new CriteriaQuery(getRecentPriceQuery());
                return elasticsearchOperations.count(query, CURRENT_INDEX);
        }

        static Set<String> computeMissingIds(Collection<Long> ids, Map<String, ?> existingResults) {
                return ids.stream()
                                .map(String::valueOf)
                                .filter(id -> !existingResults.containsKey(id))
                                .collect(Collectors.toSet());
        }

        @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
        public Long countMainIndexHavingVertical() {
                CriteriaQuery query = new CriteriaQuery(new Criteria("vertical").exists());
                return elasticsearchOperations.count(query, CURRENT_INDEX);
        }

        /**
         * Count recent products with offers, a registered impact score, and not excluded from the catalogue.
         *
         * @return count of recent products with an ImpactScore
         */
        @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
        public Long countMainIndexHavingImpactScore() {
                Criteria criteria = getRecentPriceQuery()
                        .and(new Criteria("excluded").is(false))
                        .and(new Criteria("scores.ECOSCORE.value").exists());
                CriteriaQuery query = new CriteriaQuery(criteria);
                return elasticsearchOperations.count(query, CURRENT_INDEX);
        }

        /**
         * Count recent products with offers that are missing a vertical assignment.
         *
         * @return count of recent products without a vertical
         */
        @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
        public Long countMainIndexWithoutVertical() {
                Criteria criteria = getRecentPriceQuery()
                        .and(new Criteria("vertical").exists().not());
                CriteriaQuery query = new CriteriaQuery(criteria);
                return elasticsearchOperations.count(query, CURRENT_INDEX);
        }

        /**
         * Count recent products for a specific vertical.
         *
         * @param vertical vertical identifier
         * @return count of recent products with offers for the vertical
         */
        @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
        public Long countMainIndexHavingVertical(String vertical) {
                CriteriaQuery query = new CriteriaQuery(getRecentPriceQuery().and(new Criteria("vertical").is(vertical)));
                return elasticsearchOperations.count(query, CURRENT_INDEX);
        }

        @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
        public Long countMainIndexHavingScore(String scoreName, String vertical) {
                CriteriaQuery query = new CriteriaQuery(new Criteria("vertical").is(vertical) .and(new Criteria("scores." + scoreName + ".value").exists()));
                return elasticsearchOperations.count(query, CURRENT_INDEX);
        }

        /**
         * Count recent products with a score registered for a vertical.
         *
         * @param scoreName the score name under {@code scores.<name>.value}
         * @param vertical the vertical identifier
         * @return count of recent, non-excluded products having the score
         */
        @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
        public Long countMainIndexHavingScoreWithFilters(String scoreName, String vertical) {
                String scoreField = "scores." + scoreName + ".value";
                Criteria criteria = getRecentPriceQuery()
                        .and(new Criteria("vertical").is(vertical))
                        .and(new Criteria("excluded").is(false))
                        .and(new Criteria(scoreField).exists());
                CriteriaQuery query = new CriteriaQuery(criteria);
                return elasticsearchOperations.count(query, CURRENT_INDEX);
        }

        /**
         * Count recent products with the given score field matching a threshold filter.
         *
         * @param scoreName the score name under {@code scores.<name>.value}
         * @param vertical the vertical identifier
         * @param operator the threshold operator to apply
         * @param thresholdValue the numeric threshold to compare against
         * @return the count of products matching the threshold
         */
        @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
        public Long countMainIndexHavingScoreThreshold(String scoreName, String vertical, SubsetCriteriaOperator operator, double thresholdValue) {
                String scoreField = "scores." + scoreName + ".value";
                Criteria criteria = getRecentPriceQuery()
                        .and(new Criteria("vertical").is(vertical))
                        .and(new Criteria("excluded").is(false))
                        .and(new Criteria(scoreField).exists());

                Criteria thresholdCriteria = applyScoreThreshold(new Criteria(scoreField), operator, thresholdValue);
                CriteriaQuery query = new CriteriaQuery(criteria.and(thresholdCriteria));
                return elasticsearchOperations.count(query, CURRENT_INDEX);
        }

        private Criteria applyScoreThreshold(Criteria criteria, SubsetCriteriaOperator operator, double thresholdValue) {
                return switch (operator) {
                case LOWER_THAN -> criteria.lessThan(thresholdValue);
                case GREATER_THAN -> criteria.greaterThan(thresholdValue);
                case EQUALS -> criteria.is(thresholdValue);
                };
        }

        @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
        public Long countMainIndexExcluded() {
            CriteriaQuery query = new CriteriaQuery(new Criteria("excluded").is(true));
            return elasticsearchOperations.count(query, CURRENT_INDEX);
        }

        /**
         * Count recent products with offers, AI reviews for the requested locale, and not excluded from the catalogue.
         *
         * @param locale locale to check (e.g. {@code fr}); defaults to {@code default} when blank
         * @return count of recent products with AI reviews
         */
        @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
        public Long countMainIndexValidAndReviewed(String locale) {
            CriteriaQuery query = new CriteriaQuery(getRecentPriceQuery()
                    .and(new Criteria("excluded").is(false))
                    .and(new Criteria(resolveReviewField(locale)).exists()));
            return elasticsearchOperations.count(query, CURRENT_INDEX);
        }

        /**
         * Count recent products with offers, a valid ECOSCORE, and not excluded from the catalogue.
         *
         * @return count of recent products with an ECOSCORE
         */
        @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
        public Long countMainIndexValidAndRated() {
            CriteriaQuery query = new CriteriaQuery(getRecentPriceQuery()
                    .and(new Criteria("excluded").is(false))
                    .and(new Criteria("scores.ECOSCORE.value").exists()));
            return elasticsearchOperations.count(query, CURRENT_INDEX);
        }

        @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
        public Long countMainIndexTotal(String vertical) {
            CriteriaQuery query = new CriteriaQuery(new Criteria("vertical").is(vertical));
            return elasticsearchOperations.count(query, CURRENT_INDEX);
        }

        @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
        public Long countMainIndexExcluded(String vertical) {
            CriteriaQuery query = new CriteriaQuery(new Criteria("vertical").is(vertical).and(new Criteria("excluded").is(true)));
            return elasticsearchOperations.count(query, CURRENT_INDEX);
        }

        /**
         * Count recent products with offers, AI reviews for the requested locale, and not excluded for a specific vertical.
         *
         * @param vertical vertical identifier
         * @param locale locale to check (e.g. {@code fr}); defaults to {@code default} when blank
         * @return count of recent products with AI reviews for the vertical
         */
        @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
        public Long countMainIndexValidAndReviewed(String vertical, String locale) {
            CriteriaQuery query = new CriteriaQuery(getRecentPriceQuery()
                    .and(new Criteria("vertical").is(vertical))
                    .and(new Criteria("excluded").is(false))
                    // TODO : Does not works because fiels is not indexed in product mapping (    "reviews": {
//                    "type": "object",
//                    "enabled": false
//                 },
                    // How to proceed ?
                    .and(new Criteria(resolveReviewField(locale)).exists()));
            return elasticsearchOperations.count(query, CURRENT_INDEX);
        }

        private String resolveReviewField(String locale) {
            String resolvedLocale = locale;
            if (resolvedLocale == null || resolvedLocale.isBlank()) {
            	// TODO : Handle i18n
                resolvedLocale = "fr";
            }
            // TODO : Weak, relying on baseline...
            return "reviews." + resolvedLocale + ".review.baseLine";
        }

        @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
        public Long countMainIndexValidAndRated(String vertical) {
            CriteriaQuery query = new CriteriaQuery(getRecentPriceQuery()
                    .and(new Criteria("vertical").is(vertical))
                    .and(new Criteria("excluded").is(false))
                    .and(new Criteria("scores.ECOSCORE.value").exists()));
            return elasticsearchOperations.count(query, CURRENT_INDEX);
        }

        /**
         * Compute absolute cardinality statistics for a score within a vertical.
         *
         * @param scoreName score name to aggregate
         * @param vertical  vertical identifier
         * @return cardinality statistics or {@code null} if the score is missing
         */
        @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
        public org.open4goods.model.rating.Cardinality scoreAbsoluteCardinality(String scoreName, String vertical) {
                return scoreCardinalityForField(scoreName, vertical, "scores." + scoreName + ".absolute.value");
        }

        /**
         * Compute relative cardinality statistics for a score within a vertical.
         *
         * @param scoreName score name to aggregate
         * @param vertical  vertical identifier
         * @return cardinality statistics or {@code null} if the score is missing
         */
        @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
        public org.open4goods.model.rating.Cardinality scoreRelativCardinality(String scoreName, String vertical) {
                return scoreCardinalityForField(scoreName, vertical, "scores." + scoreName + ".relativ.value");
        }

        /**
         * Aggregate score values for the given field path using Elasticsearch extended stats.
         *
         * @param scoreName score name used for logging
         * @param vertical  vertical identifier
         * @param fieldPath Elasticsearch field path to aggregate
         * @return cardinality statistics or {@code null} if the aggregation is empty
         */
        private org.open4goods.model.rating.Cardinality scoreCardinalityForField(String scoreName, String vertical, String fieldPath) {
                Criteria criteria = getRecentPriceQuery()
                                .and(new Criteria("vertical").is(vertical))
                                .and(new Criteria(fieldPath).exists());

                String aggregationName = "score_stats";
                NativeQueryBuilder queryBuilder = new NativeQueryBuilder()
                                .withQuery(new CriteriaQuery(criteria))
                                .withAggregation(aggregationName, Aggregation.of(a -> a.extendedStats(e -> e.field(fieldPath))))
                                .withPageable(PageRequest.of(0, 1));

                SearchHits<Product> results = search(queryBuilder.build(), ProductRepository.MAIN_INDEX_NAME);
                if (results == null || results.getAggregations() == null) {
                        logger.debug("No aggregation results for score {} in vertical {}", scoreName, vertical);
                        return null;
                }

                if (!(results.getAggregations() instanceof ElasticsearchAggregations aggregations)) {
                        logger.debug("Unexpected aggregation container for score {} in vertical {}", scoreName, vertical);
                        return null;
                }

                ExtendedStatsAggregate stats = aggregations.get(aggregationName).aggregation().getAggregate().extendedStats();
                if (stats == null || stats.count() == 0) {
                        return null;
                }

                org.open4goods.model.rating.Cardinality cardinality = new org.open4goods.model.rating.Cardinality();
                cardinality.setMin(stats.min());
                cardinality.setMax(stats.max());
                cardinality.setAvg(stats.avg());
                cardinality.setSum(stats.sum());
                cardinality.setSumOfSquares(stats.sumOfSquares());
                cardinality.setCount(Math.toIntExact(stats.count()));
                return cardinality;
        }


	@Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public Long countMainIndexHavingRecentUpdate() {
		CriteriaQuery query = new CriteriaQuery(getRecentPriceQuery());
		return elasticsearchOperations.count(query, CURRENT_INDEX);
	}

    @Cacheable(cacheNames = CacheConstants.ONE_DAY_LOCAL_CACHE_NAME)
    public long countItemsByBarcodeType(BarcodeType... barcodeTypes) {
        Criteria criteria = new Criteria("gtinInfos.upcType").in((Object[]) barcodeTypes);
        CriteriaQuery query = new CriteriaQuery(criteria);
        return elasticsearchOperations.count(query, CURRENT_INDEX);
    }

	@Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public Map<Integer, Long> byTaxonomy() {

		// Setting the query
		NativeQueryBuilder esQuery = new NativeQueryBuilder().withQuery(new CriteriaQuery( new Criteria("id").exists()));

		// Adding standard aggregations
		esQuery = esQuery
				.withAggregation("taxonomy", 	Aggregation.of(a -> a.terms(ta -> ta.field("googleTaxonomyId").size(50000))  ))
				;

		SearchHits<Product> results = search(esQuery.build(),ProductRepository.MAIN_INDEX_NAME);


		// Handling aggregations results if relevant
		//TODO(p3,safety) : this cast should be avoided
		ElasticsearchAggregations aggregations = (ElasticsearchAggregations)results.getAggregations();


		///////
		// Numeric aggregations
		///////
		LongTermsAggregate taxonomy = aggregations.get("taxonomy").aggregation().getAggregate().lterms();

		Map<Integer, Long> ret = new HashMap<>();
		for (LongTermsBucket b : taxonomy.buckets().array()) {
			ret.put(new Long(b.key()).intValue(), b.docCount());
		}


		return ret;

	}

	/**
	 * Return all products matching the vertical in the config or already having a
	 * vertical defined
	 *
	 * @param v
	 * @return
	 */
	// TODO : Could add datasourcename in a virtual "all", then apply the logic filter to batch get all categories matching....
	public Stream<Product> getProductsMatchingCategoriesOrVerticalId(VerticalConfig v) {

		// We match larger, on all matching categories cause those fields are not indexed
		Set<String> datasources = new HashSet<String>();
		v.getMatchingCategories().values().forEach(cat -> {
			cat.forEach(elem -> {
				datasources.add(elem);
			});
		});

		Criteria c = new Criteria("datasourceCategories").in(datasources)
				.or(new Criteria("vertical").is(v.getId()));

		final NativeQuery initialQuery = new NativeQueryBuilder().withQuery(new CriteriaQuery(c)).build();

		return getElasticsearchOperations()
				.searchForStream(initialQuery, Product.class, ProductRepository.CURRENT_INDEX).stream()
				.map(SearchHit::getContent);
				// We have all categories matching, refine here to match the standard agg behaviour
//				.filter(e -> {
//					VerticalConfig cat = getVerticalForCategories(e.getCategoriesByDatasources());
//					if (null != cat && cat.getId().equals(v.getId())) {
//						return true;
//					}
//					return false;
//				});


	}

	/**
	 * Bulk update, using Document
	 * @param partialItemsResults
	 */
	public void bulkUpdateDocument(Collection<ProductPartialUpdateHolder> partialItemsResults) {
	    List<UpdateQuery> updateQueries = partialItemsResults.stream()
	        .map(product -> {
	            Map<String, Object> fieldsToUpdate = product.getChanges();
	            return UpdateQuery.builder(String.valueOf(product.getProductId()))
	                .withDocument(Document.from(fieldsToUpdate))
	                .withIndex(CURRENT_INDEX.getIndexName())
	                .build();
	        })
	        .collect(Collectors.toList());

	    // Perform the bulk update
	    elasticsearchOperations.bulkUpdate(updateQueries, CURRENT_INDEX);
	}

//	/**
//	 * Bulk update, using script
//	 * @param partialItemsResults
//	 */
//	public void bulkUpdateScript(Set<ProductPartialUpdateHolder> partialItemsResults) {
//	    // Prepare a list to hold the update queries
//	    List<UpdateQuery> updateQueries = partialItemsResults.stream()
//	        .map(product -> {
//	            // Script to iterate over the map and update the fields in _source
//	            String script = "for (entry in params.fieldsToUpdate.entrySet()) { ctx._source[entry.getKey()] = entry.getValue(); }";
//
//	            // Pass the updated fields as parameters
//	            Map<String, Object> params = new HashMap<>();
//	            params.put("fieldsToUpdate", product.getChanges());
//
//	            // Create and return the UpdateQuery with the script and parameters
//	            return UpdateQuery.builder(String.valueOf(product.getProductId()))
//	                .withScript(script)
//	                .withParams(params)
//	                .withIndex(current_index.getIndexName())  // Use the current index
//	                .build();
//	        })
//	        .collect(Collectors.toList());
//
//	    // Perform the bulk update
//	    elasticsearchOperations.bulkUpdate(updateQueries, current_index);
//	}
//


	/**
	 *
	 * @return Criteria representing recent prices
	 */
    public Criteria getRecentPriceQuery() {
                return getRecentProducts().and(new Criteria("offersCount").greaterThan(0));

        }

        /**
         * Executes a KNN search on the embedding field while enforcing recency and offer availability guardrails.
         *
         * @param vector the embedding vector to search with
         * @param baseFilters optional additional filters to apply
         * @param k number of neighbours to retrieve
         * @return search hits ordered by vector similarity
         */
        public SearchHits<Product> knnSearchByEmbedding(float[] vector, Criteria baseFilters, int k)
        {
                Objects.requireNonNull(vector, "Embedding vector is required");
                if (vector.length == 0)
                {
                        throw new IllegalArgumentException("Embedding vector must not be empty");
                }

                Criteria effectiveCriteria = (baseFilters == null ? new Criteria() : baseFilters).and(getRecentPriceQuery());

                List<Float> queryVector = new ArrayList<>(vector.length);
                for (float value : vector)
                {
                        queryVector.add(value);
                }

                KnnSearch knnSearch = KnnSearch.of(knn -> knn
                                .field("embedding")
                                .queryVector(queryVector)
                                .k(k)
                                .numCandidates(Math.max(k * 2, 50))
                );

                NativeQuery knnQuery = new NativeQueryBuilder()
                                .withQuery(new CriteriaQuery(effectiveCriteria))
                                .withKnnSearches(knnSearch)
                                .withPageable(PageRequest.of(0, k))
                                .build();

                return elasticsearchOperations.search(knnQuery, Product.class, CURRENT_INDEX);
        }

	/**
	 *
	 * @return Criteria representing recent updated products
	 */
	public Criteria getRecentProducts() {
		return new Criteria("lastChange").greaterThan(expirationClause());
	}

	/**
	 *
	 * @return Criteria representing the valid dates
	 */
	public Criteria getSinceDateQuery(Long epoch) {
		return new Criteria("lastChange").greaterThan(epoch);
	}

	/**
	 *
	 * @return the date from when aggregateddatas are considered to be valid one
	 */
	public long expirationClause() {
		return System.currentTimeMillis() - VALID_UNTIL_DURATION;
	}

	/**
	 * save the product in redis * @param result
	 */
	private void saveToRedis(Product result) {
//		executor.submit(() -> {
//			redisRepository.save(result);
//		});
	}

	public BlockingQueue<Product> getFullProductQueue() {
		return fullProductQueue;
	}

	public BlockingQueue<ProductPartialUpdateHolder> getPartialProductQueue() {
		return partialProductQueue;
	}

	public void delete(Product p) {
		elasticsearchOperations.delete(p.gtin(), CURRENT_INDEX);

	}

	public ElasticsearchOperations getElasticsearchOperations() {
		return elasticsearchOperations;
	}





}
