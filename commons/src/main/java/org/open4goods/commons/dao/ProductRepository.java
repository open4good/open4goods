package org.open4goods.commons.dao;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.exceptions.ResourceNotFoundException;
import org.open4goods.commons.model.constants.CacheConstants;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.store.repository.ProductIndexationWorker;
import org.open4goods.commons.store.repository.redis.RedisProductRepository;
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
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;

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

	
	// The file queue implementation
	// TODO(p3,conf) : Limit from conf
	private BlockingQueue<Product> queue = new LinkedBlockingQueue<>(150000);
	
	
	/**
	 * !!!MAJOR CONST !!! Duration in ms where a price is considered to be valid. Only data with a
	 * price greater than this one will be returned to the user. Also defines the caching TTL of redis
	 **/
	// TODO(gof) : from conf
	public final static long VALID_UNTIL_DURATION = 1000 * 3600 * 24 * 2;

	private static final int MAX_TITLE_ITEMS_TO_FETCH = 5;

	public IndexCoordinates current_index = IndexCoordinates.of(MAIN_INDEX_NAME);

	private @Autowired ElasticsearchOperations elasticsearchTemplate;

	private @Autowired RedisProductRepository redisRepository;
	
//	private @Autowired RedisOperations<String, Product> redisRepo;

	public ProductRepository() {
		
		// TODO : from conf
		int dequeueSize = 20;
		int workers = 3;
		int pauseDuration = 5000;
		
		logger.info("Starting file queue consumer thread, with bulk page size of {} items", dequeueSize );
				
		for (int i = 0; i < workers; i++) {			
			Thread.startVirtualThread((new ProductIndexationWorker(this, dequeueSize, pauseDuration,"dequeue-worker-"+i)));
		}
	}

//	private ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	/**
	 * Return all products matching the vertical in the config or already having a
	 * vertical defined
	 * 
	 * @param v
	 * @return
	 */
	public Stream<Product> getProductsMatchingCategories(VerticalConfig v) {
		Criteria c = new Criteria("datasourceCategories").in(v.getMatchingCategories())
		// TODO : Add exclusion
//				.and(new Criteria("datasourceCategories").notIn(v.getMatchingCategories()))
				.or(new Criteria("vertical").is(v.getId()));
// TODO : Add or get by taxonomyId
		
		
		final NativeQuery initialQuery = new NativeQueryBuilder().withQuery(new CriteriaQuery(c)).build();

		return elasticsearchTemplate.searchForStream(initialQuery, Product.class, current_index).stream()
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
	    return elasticsearchTemplate.searchForStream(query, Product.class, current_index)
	    		.stream()
	    		// TODO : Check CPU usage
	    		.parallel()
	            .map(SearchHit::getContent);
	}

	public Stream<Product> searchInValidPrices(String query, final String indexName, int from, int to) {

		Criteria c = new Criteria().expression(query).and(getValidDateQuery());

		final NativeQuery initialQuery = new NativeQueryBuilder().withQuery(new CriteriaQuery(c))
				.withPageable(PageRequest.of(from, to)).build();

		return elasticsearchTemplate.search(initialQuery, Product.class, current_index).stream()
				.map(SearchHit::getContent);

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

		
		
		
		Criteria c = getValidDateQuery()
				.and( new Criteria("vertical").is(vertical.getId()))

				//				.or(new Criteria("datasourceCategories").in(vertical.getMatchingCategories())
				;
		
		if (!withExcluded) {
            c = c.and(new Criteria("excluded").is(false));
        }
		
		final NativeQuery initialQuery = new NativeQueryBuilder()
				.withQuery(new CriteriaQuery(c)).build();
		return elasticsearchTemplate.searchForStream(initialQuery, Product.class, current_index).stream()
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
		
		return elasticsearchTemplate.searchForStream(initialQuery, Product.class, current_index).stream().map(SearchHit::getContent);
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
				.and(getValidDateQuery())
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
		
		return elasticsearchTemplate.searchForStream(initialQuery, Product.class, current_index).stream().map(SearchHit::getContent);
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
		return elasticsearchTemplate.search(query, Product.class, IndexCoordinates.of(indexName));

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
			//TODO : timeout from conf
			queue.offer(p, 300, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
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
	public void index(Collection<Product> data) {

		logger.info("Queuing {} products", data.size());
		
		data.forEach(e -> {
			
			try {
				if (!queue.offer(e, 300, TimeUnit.SECONDS)) {
					logger.error("!!!! Cannot enqueue product {}",e);
				}
			} catch (InterruptedException e1) {
				logger.error("!!!! exception, cannot enqueue product {}",e);
			}
			
		});
		

	}

	
	public void store(Collection<Product> data) {
		logger.info("Indexing {} products", data.size());

//		executor.submit(() -> {
			elasticsearchTemplate.save(data, current_index);
//		});

//		executor.submit(() -> {
			redisRepository.saveAll(data);
//			redisRepo.opsForValue().multiSet(data.stream().collect(Collectors.toMap(Product::gtin, Function.identity())));
//		});
	}
	
	public void storeNoCache(Collection<Product> data) {
		logger.info("Indexing without caching {} products", data.size());

		elasticsearchTemplate.save(data, current_index);

	}
	
	
	public void forceIndex(Product data) {
		logger.info("Indexing  product {}", data.gtin());

//		executor.submit(() -> {
			elasticsearchTemplate.save(data, current_index);
//		});

//		executor.submit(() -> {
//			redisRepo.opsForValue().set(data.gtin(), data);
			redisRepository.save(data);
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
	public Product getById(final String productId) throws ResourceNotFoundException {

		logger.info("Getting product  {}", productId);
		// Getting from redis
		
		
		
//		Product result = redisRepo.opsForValue().get(productId);
		Product result;
		try {
			result = redisRepository.findById(productId).orElseThrow(ResourceNotFoundException::new);
		} catch (ResourceNotFoundException e) {
			
			result = null;
			
		} catch (Exception e) {
			logger.error("Error getting product {} from redis", productId, e);
			result = null;
		}

		if (null == result) {
			// Fail, getting from elastic
			logger.info("Cache miss, getting product {} from elastic", productId);
			result = elasticsearchTemplate.get(productId, Product.class);

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
		NativeQueryBuilder esQuery = new NativeQueryBuilder().withQuery(new CriteriaQuery(new Criteria("names.offerNames").matchesAll(words) ));
		SearchHits<Product> results = search(esQuery.withPageable(PageRequest.of(0, maxItems)).build(),ProductRepository.MAIN_INDEX_NAME);
		return results.stream().map(SearchHit::getContent).collect(Collectors.toList());
		
	}
	
	/**
	 * Get multiple data from ids
	 * @param ids
	 * @return
	 * @throws ResourceNotFoundException
	 */
	public Map<String, Product> multiGetById( final Collection<String> ids)
			throws ResourceNotFoundException {

		logger.info("Getting {} products from default index",ids.size());
		Map<String, Product> ret = new HashMap<String, Product>();
		
		
		// Getting from redis
//		Iterable<Product> redisResults = redisRepo.opsForValue().multiGet(ids);
		Iterable<Product> redisResults = redisRepository.findAllById(ids);
		redisResults.forEach(e -> {
			if (null != e) {
				ret.put(e.gtin(), e);
			}
		});
		
		// Getting the one we don't have in redis from elastic 		
		Set<String> missingIds = ids.stream().filter(e -> !ret.containsKey(e)).collect(Collectors.toSet());
		logger.info("redis hits : {}, missing : {}, queue size : {}",ret.size(), missingIds.size(),queue.size());
		
		
		if (missingIds.size() != 0) {
			
			
			NativeQuery query = new NativeQueryBuilder().withIds(missingIds).build();
	
			elasticsearchTemplate.multiGet(query, Product.class,current_index )
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
				
			redisRepository.saveAll(redisItems);
//				redisRepo.opsForValue().multiSet();
//			});
		}
		
		return ret;
	}


	@Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public Long countMainIndex() {
		return elasticsearchTemplate.count(Query.findAll(), current_index);
	}

	@Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public Long countMainIndexHavingPrice() {
		CriteriaQuery query = new CriteriaQuery(getValidDateQuery());
		return elasticsearchTemplate.count(query, current_index);
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
		//TODO(gof) : this cast should be avoided
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
	 *
	 * @return Criteria representing the valid dates
	 */
	public Criteria getValidDateQuery() {
		return new Criteria("price.minPrice.timeStamp").greaterThan(expirationClause());
	}

	/**
	 *
	 * @return Criteria representing the valid dates
	 */
	public Criteria getSinceDateQuery(Long epoch) {
		return new Criteria("price.minPrice.timeStamp").greaterThan(epoch);
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
//			redisRepo.opsForValue().set(result.gtin(), result);
			redisRepository.save(result);
//		});
	}

	public BlockingQueue<Product> getQueue() {
		return queue;
	}


}
