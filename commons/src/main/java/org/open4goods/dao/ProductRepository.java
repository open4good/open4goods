package org.open4goods.dao;

import java.util.*;
import java.util.stream.Stream;

import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
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

/**
 * The Elastic Data Access Object for products
 *
 * @author goulven
 *
 */
public class ProductRepository {


	private static final Logger logger = LoggerFactory.getLogger(ProductRepository.class);

	public static final String MAIN_INDEX_NAME = Product.DEFAULT_REPO;

	/** Duration in ms where a price is considered to be valid. Only data with a price greater than this one will be returned to the user **/
	//TODO(gof) : from conf
	private long VALID_UNTIL_DURATION =1000 * 3600 * 24 * 2;

	public IndexCoordinates current_index = IndexCoordinates.of(MAIN_INDEX_NAME);

	private @Autowired ElasticsearchOperations elasticsearchTemplate;


	public ProductRepository () {
	}



	/**
	 * Export all aggregated data
	 * @return
	 */
	public Stream<Product> exportAll() {
		return elasticsearchTemplate.searchForStream(Query.findAll(), Product.class, current_index).stream().map(SearchHit::getContent);
	}


	public Stream<Product> searchInValidPrices(String query, final String indexName, int from, int to) {

		Criteria c = new Criteria().expression(query)
				.and(getValidDateQuery());

		final NativeQuery initialQuery = new NativeQueryBuilder()
				.withQuery(new CriteriaQuery(c))
				.withPageable(PageRequest.of(from, to))
				.build();

		return elasticsearchTemplate.search(initialQuery, Product.class, current_index)
				.stream()
				.map(SearchHit::getContent);

	}

	/**
	 * Export all aggregateddatas for a vertical
	 * @param vertical
	 * @param max
	 * @param indexName
	 * @return
	 */
	public Stream<Product> exportVerticalWithValidDate(String vertical) {

		
		Criteria c = new Criteria("vertical").is(vertical)
				.and(getValidDateQuery());

		final NativeQuery initialQuery = new NativeQueryBuilder()
				.withQuery(new CriteriaQuery(c))
				.build();

		return elasticsearchTemplate.searchForStream(initialQuery, Product.class, current_index).stream().map(SearchHit::getContent);

	}

	public SearchHits<Product> search(Query query, final String indexName) {
		return elasticsearchTemplate.search(query, Product.class, IndexCoordinates.of(indexName));

	}

	/**
	 * Index an Product
	 *
	 * @param p
	 */
	public void index(final Product p, final String indexName) {
		elasticsearchTemplate.save(p,IndexCoordinates.of(indexName));
	}

	/**
	 * Index an Product
	 *
	 * @param p
	 */
	public void index(final Product p) {
		elasticsearchTemplate.save(p,current_index);
	}
	/**
	 * Bulk Index multiple Product
	 *
	 * @param p
	 */
	public void index(Collection<Product> data, final String indexName) {
		elasticsearchTemplate.save(data,IndexCoordinates.of(indexName));
	}

	/**
	 * Bulk Index multiple Product
	 *
	 * @param p
	 */
	public void index(Collection<Product> data) {
		elasticsearchTemplate.save(data,current_index);
	}

	//	/**
	//	 * Bulk Index multiple Product
	//	 *
	//	 * @param p
	//	 */
	//	public void indexBackup(Set<Product> data) {
	//		elasticsearchTemplate.save(data,backup_index);
	//	}

	//	/**
	//	 * Reinitialize an Product elastic index (delete then re-create with)
	//	 * @param indexName
	//	 */
	//	@SuppressWarnings("rawtypes")
	//	public void initIndex(final String indexName) {
	//		try {
	//			IndexOperations indexOps = elasticsearchTemplate.indexOps(IndexCoordinates.of(indexName));
	//			if (!indexOps.exists() ) {
	//				final Map mapping =    elasticsearchTemplate.indexOps(IndexCoordinates.of(Product.DEFAULT_REPO) ). getMapping();
	//				InputStream stream = resolver.getResource("classpath:/elastic-settings.json").getInputStream();
	//				Settings settings =  Settings.parse(IOUtils.toString(stream, Charset.defaultCharset()));
	//				IOUtils.closeQuietly(stream);
	//
	//
	//				logger.info("Will create the Product typed index for {}", indexName);
	//				elasticsearchTemplate.indexOps(IndexCoordinates.of(indexName)).create(settings,Document.from(mapping));
	//
	////				elasticsearchTemplate.indexOps(IndexCoordinates.of(indexName)).putMapping(Document.from(mapping));
	//
	//			}
	//		} catch (final Exception e) {
	//			logger.error("Error while re-creating index  {} : {}", indexName, e.getMessage());
	//		}
	//	}

	//	public void deleteIndex(final String indexName) {
	//		try {
	//			IndexOperations indexOps = elasticsearchTemplate.indexOps(IndexCoordinates.of(indexName));
	//			indexOps.delete();
	//
	//		} catch (final Exception e) {
	//			logger.error("Error while deleting index  {} : {}", indexName, e.getMessage());
	//		}
	//	}

	/**
	 * Return an aggregated data by it's ID
	 * @param productId
	 * @param indexName
	 * @return
	 * @throws ResourceNotFoundException
	 */
	public Product getById( final String productId, String indexName)
			throws ResourceNotFoundException {

		final Product result = elasticsearchTemplate.get(productId,Product.class,IndexCoordinates.of(indexName));

		if (null == result) {
			throw new ResourceNotFoundException("Product '" + productId + "' does not exists");
		}

		return result;

	}

	/**
	 * Get multiple data from ids
	 * @param ids
	 * @return
	 * @throws ResourceNotFoundException
	 */
	public Map<String, Product> multiGetById( final Collection<String> ids)
			throws ResourceNotFoundException {

		NativeQuery query = new NativeQueryBuilder().withIds(ids).build();

		Map<String, Product> ret = new HashMap<String, Product>();
		elasticsearchTemplate.multiGet(query, Product.class,current_index )
		.stream().map(MultiGetItem::getItem)
		.filter(Objects::nonNull)
		.forEach(e -> ret.put(e.gtin(), e));

		return ret;

	}



	@Cacheable( cacheNames=CacheConstants.ONE_MINUTE_LOCAL_CACHE_NAME)
	public Product getById(String productId) throws ResourceNotFoundException {
		return getById(productId, MAIN_INDEX_NAME);

	}

	@Cacheable(key = "#root.method.name", cacheNames=CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public Long countMainIndex() {
		return elasticsearchTemplate.count(Query.findAll(), current_index);
	}


	@Cacheable(key = "#root.method.name", cacheNames=CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public Long countMainIndexHavingPrice() {
		CriteriaQuery query = new CriteriaQuery(getValidDateQuery());
		return elasticsearchTemplate.count(query, current_index);
	}


	/**
	 *
	 * @return Criteria representing the valid dates
	 */
	public Criteria getValidDateQuery() {
		return new Criteria("price.minPrice.timeStamp").greaterThan(expirationClause()) ;
	}

	/**
	 *
	 * @return the date from when aggregateddatas are considered to be valid one
	 */
	public long expirationClause() {
		return System.currentTimeMillis() - VALID_UNTIL_DURATION;
	}

}
