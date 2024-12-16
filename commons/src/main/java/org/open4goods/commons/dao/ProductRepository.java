package org.open4goods.commons.dao;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.open4goods.commons.config.yml.IndexationConfig;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.exceptions.ResourceNotFoundException;
import org.open4goods.commons.model.constants.CacheConstants;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.model.product.ProductPartialUpdateHolder;
import org.open4goods.commons.services.SerialisationService;
import org.open4goods.commons.store.repository.FullProductIndexationWorker;
import org.open4goods.commons.store.repository.PartialProductIndexationWorker;
import org.open4goods.model.BarcodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;

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


	private @Autowired SerialisationService serialisationService;

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
	    // TODO : From conf, apply to other
	    query.setPageable(PageRequest.of(0, 10000)); // Fetch larger batches
	    return elasticsearchOperations.searchForStream(query, Product.class, CURRENT_INDEX)
	    		.stream()
	    		// TODO : Check CPU usage
	    		.parallel()
	            .map(SearchHit::getContent);
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
	            .withSourceFilter(new FetchSourceFilter(new String[]{"categoriesByDatasources"}, null));
	    
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
	public Stream<Product> exportAll(BarcodeType... barcodeTypes) {
		
		Criteria criteria = new Criteria("gtinInfos.upcType").in((Object[]) barcodeTypes);
		CriteriaQuery query = new CriteriaQuery(criteria);
		
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
		
		vConf.getAvailableImpactScoreCriterias().entrySet().forEach(criteria -> {
			
			Long count = countMainIndexHavingScore(criteria.getKey(),vConf.getId());

			// TODO(p2, conf) : threshold from conf
			if (count > 10) {
				ret.put(criteria.getKey() ,  count);
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
		
		initialQueryBuilder =  initialQueryBuilder.withSort(Sort.by(org.springframework.data.domain.Sort.Order.desc("scores.ECOSCORE.value")));									

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

		Criteria c = new Criteria("vertical").is(vertical)
				.and(getRecentPriceQuery())
				;

		if (!withExcluded) {
            c = c.and(new Criteria("excluded").is(false));
        }
		
		NativeQueryBuilder initialQueryBuilder = new NativeQueryBuilder().withQuery(new CriteriaQuery(c));
		
		initialQueryBuilder =  initialQueryBuilder.withSort(Sort.by(org.springframework.data.domain.Sort.Order.desc("scores.ECOSCORE.value")));									
		if (null != max) {
			initialQueryBuilder = initialQueryBuilder.withMaxResults(max);
		}

		NativeQuery initialQuery = initialQueryBuilder.build();
		
		return elasticsearchOperations.searchForStream(initialQuery, Product.class, CURRENT_INDEX).stream().map(SearchHit::getContent);
	}


	public Stream<Product> getAllHavingVertical() {
		Criteria c = new Criteria("vertical").exists()
				;


		NativeQueryBuilder initialQueryBuilder = new NativeQueryBuilder().withQuery(new CriteriaQuery(c));

		initialQueryBuilder =  initialQueryBuilder.withSort(Sort.by(org.springframework.data.domain.Sort.Order.desc("scores.ECOSCORE.value")));

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

	
	
	public SearchHits<Product> search(Query query, final String indexName) {
		return elasticsearchOperations.search(query, Product.class, IndexCoordinates.of(indexName));

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

		if (null == result) {
			// Fail, getting from elastic
			logger.info("Cache miss, getting product {} from elastic", productId);
			result = elasticsearchOperations.get(String.valueOf(productId), Product.class);

			if (null == result) {
				throw new ResourceNotFoundException("Product '" + productId + "' does not exists");
			}

			// found, adding it in redis cache
			saveToRedis(result);
		} else {
			logger.info("Cache hit, got product {} from redis", productId);
		}

		
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
		Set<String> missingIds = ids.stream().filter(e -> !ret.containsKey(e)).map(e-> String.valueOf(e)) .collect(Collectors.toSet());
		logger.info("redis hits : {}, missing : {}, queue size : {}",ret.size(), missingIds.size(),fullProductQueue.size());
		
		
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

	@Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public Long countMainIndexHavingScore(String scoreName, String vertical) {
		CriteriaQuery query = new CriteriaQuery(new Criteria("vertical").is(vertical) .and(new Criteria("scores." + scoreName + ".value").exists()));
		return elasticsearchOperations.count(query, CURRENT_INDEX);
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
