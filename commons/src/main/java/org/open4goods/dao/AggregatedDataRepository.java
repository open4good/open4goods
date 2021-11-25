package org.open4goods.dao;

import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.product.AggregatedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.util.CloseableIterator;

/**
 * The Elastic Data Access Object for products
 *
 * @author goulven
 *
 */
public class AggregatedDataRepository {

	private static final int BACKUP_BULK_SIZE = 200;

	public static final int NUMBER_OF_SAMPLES_DATA = 100;

	private static final Logger logger = LoggerFactory.getLogger(AggregatedDataRepository.class);

	private static final String MAIN_INDEX_NAME = "all";

	private static final String BAKCUP_INDEX_NAME = MAIN_INDEX_NAME+"-1";


	/** Duration in ms where a price is considered to be valid. Only data with a price greater than this one will be returned to the user **/
	private long VALID_UNTIL_DURATION =1000 * 3600 * 24 * 5;

	private IndexCoordinates current_index = IndexCoordinates.of(MAIN_INDEX_NAME);

	
	private static final IndexCoordinates backup_index = IndexCoordinates.of(BAKCUP_INDEX_NAME);
	
	
	
	private @Autowired ElasticsearchRestTemplate elasticsearchTemplate;
	
	private @Autowired ResourcePatternResolver resolver;

	
	public AggregatedDataRepository () {
	}

//	/**
//	 * Stream all products
//	 *
//	 * @return
//	 */
//	public Stream<AggregatedData> exportAll(final String indexName) {
//
//		return export("*",indexName);
//	}


	public void backup () {
		
		// Performing export to backup index
		initIndex(BAKCUP_INDEX_NAME);
		Set<AggregatedData> datas= new HashSet<>();
		exportAll().forEach(e -> {
			datas.add(e);
			if (datas.size() > BACKUP_BULK_SIZE) {
				indexBackup(datas);
				datas.clear();
			}			
		});
		indexBackup(datas);
		datas.clear();
	}
	
	public Stream<AggregatedData> exportAllHavingPrices() {
		return exportAllHavingPrices(null, null);
		
	}
	public Stream<AggregatedData> exportAllHavingPrices(String sortField, SortOrder order) {
		QueryBuilder queryBuilder = QueryBuilders.boolQuery()  
				.must(getValidDateQuery());
		
		NativeSearchQueryBuilder initialQuery = new NativeSearchQueryBuilder().withQuery(queryBuilder)
//				//TODO(gof) : from conf
				.withPageable(PageRequest.of(0, 300));
		
		
			if (!StringUtils.isEmpty(sortField)) {
				initialQuery = initialQuery.withSort(SortBuilders.fieldSort(sortField).order(order));
			}
				
		final CloseableIterator<SearchHit<AggregatedData>> str = elasticsearchTemplate.searchForStream(initialQuery.build(), AggregatedData.class, current_index);

		final Stream<AggregatedData> targetStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(str, Spliterator.ORDERED), false)
				.map(e->e.getContent())	;
								
		return targetStream;
	}
	
	
	public Stream<AggregatedData> exportAll() {
		final MatchAllQueryBuilder queryBuilder = new MatchAllQueryBuilder();
		final NativeSearchQuery initialQuery = new NativeSearchQueryBuilder().withQuery(queryBuilder)
//				//TODO(gof) : from conf
				.withPageable(PageRequest.of(0, 300))					
				.build();
				
		final CloseableIterator<SearchHit<AggregatedData>> str = elasticsearchTemplate.searchForStream(initialQuery, AggregatedData.class, current_index);

		final Stream<AggregatedData> targetStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(str, Spliterator.ORDERED), false)
				.map(e->e.getContent())	;
								
		return targetStream;
	}
	
	
	public Stream<AggregatedData> searchValidPrices(String query, final String indexName, int from, int to) {

		
		QueryBuilder queryBuilder = QueryBuilders.boolQuery()  
				.must(getValidDateQuery())
				.must(new QueryStringQueryBuilder(query))
				;

		final NativeSearchQuery initialQuery = new NativeSearchQueryBuilder()
													.withQuery(queryBuilder)
													.withPageable(PageRequest.of(from, to))
													.build();
		
		
		return elasticsearchTemplate.search(initialQuery, AggregatedData.class, IndexCoordinates.of(indexName))
				.stream()
				.map(e -> e.getContent());
		
	}
	
	
	/**
	 * Index an AggregatedData
	 *
	 * @param p
	 */
	public void index(final AggregatedData p, final String indexName) {
		elasticsearchTemplate.save(p,IndexCoordinates.of(indexName));
	}

	/**
	 * Bulk Index multiple AggregatedData
	 *
	 * @param p
	 */
	public void index(Set<AggregatedData> data, final String indexName) {
		elasticsearchTemplate.save(data,IndexCoordinates.of(indexName));
	}
	
	/**
	 * Bulk Index multiple AggregatedData
	 *
	 * @param p
	 */
	public void index(Set<AggregatedData> data) {
		elasticsearchTemplate.save(data,current_index);
	}
	
	
	/**
	 * Bulk Index multiple AggregatedData
	 *
	 * @param p
	 */
	public void indexBackup(Set<AggregatedData> data) {
		elasticsearchTemplate.save(data,backup_index);
	}
	
	/**
	 * Reinitialize an AggregatedData elastic index (delete then re-create with)
	 * @param indexName
	 */
	@SuppressWarnings("rawtypes")
	public void initIndex(final String indexName) {
		try {
			IndexOperations indexOps = elasticsearchTemplate.indexOps(IndexCoordinates.of(indexName));
			if (!indexOps.exists() ) {				
				final Map mapping =    elasticsearchTemplate.indexOps(IndexCoordinates.of(AggregatedData.DEFAULT_REPO) ). getMapping();
				 Settings settings =  Settings.parse(IOUtils.toString(resolver.getResource("classpath:/elastic-settings.json").getInputStream(), Charset.defaultCharset()));

				
				
				logger.info("Will create the AggregatedData typed index for {}", indexName);				
				elasticsearchTemplate.indexOps(IndexCoordinates.of(indexName)).create(settings,Document.from(mapping));
				
//				elasticsearchTemplate.indexOps(IndexCoordinates.of(indexName)).putMapping(Document.from(mapping));
				
			}
		} catch (final Exception e) {
			logger.error("Error while re-creating index  {} : {}", indexName, e.getMessage());
		}
	}

	public void deleteIndex(final String indexName) {
		try {
			IndexOperations indexOps = elasticsearchTemplate.indexOps(IndexCoordinates.of(indexName));
			indexOps.delete();
			
		} catch (final Exception e) {
			logger.error("Error while deleting index  {} : {}", indexName, e.getMessage());
		}
	}
	
	/**
	 * Return an aggregated data by it's ID
	 * @param productId
	 * @param indexName
	 * @return
	 * @throws ResourceNotFoundException
	 */
	public AggregatedData getById( final String productId, String indexName)
			throws ResourceNotFoundException {

		final AggregatedData result = elasticsearchTemplate.get(productId,AggregatedData.class,IndexCoordinates.of(indexName));

		if (null == result) {
			throw new ResourceNotFoundException("AggregatedData '" + productId + "' does not exists");
		} 
		
		return result;
		
	}

	
	public Map<String, AggregatedData> multiGetById( final Collection<String> productId)
			throws ResourceNotFoundException {

		NativeSearchQuery query = new NativeSearchQueryBuilder().withIds(productId).build();
		
		Map<String, AggregatedData> ret = new HashMap<String, AggregatedData>();
		 elasticsearchTemplate.multiGet(query, AggregatedData.class,current_index )
				.stream().map(e -> e.getItem())
				.filter(e -> e !=null)
				.forEach(e -> {
					ret.put(e.gtin(), e);
				});
		
		return ret;
		
	}
	
	
	
	@Cacheable( cacheNames=CacheConstants.ONE_MINUTE_LOCAL_CACHE_NAME)
	public AggregatedData getById(String productId) {
		return getById(productId, MAIN_INDEX_NAME);
		
	}
	
	@Cacheable(key = "#root.method.name", cacheNames=CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public Long countMainIndex() {
		final MatchAllQueryBuilder queryBuilder = new MatchAllQueryBuilder();
		final NativeSearchQuery initialQuery = new NativeSearchQueryBuilder().withQuery(queryBuilder)
				.build();		
		return elasticsearchTemplate.count(initialQuery, current_index);
		
	}

	
	@Cacheable(key = "#root.method.name", cacheNames=CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public Long countMainIndexHavingPrice() {

		QueryBuilder queryBuilder = QueryBuilders.boolQuery()  
				.must(getValidDateQuery());
		
		final NativeSearchQuery initialQuery = new NativeSearchQueryBuilder().withQuery(queryBuilder)
				.build();		
		
		return elasticsearchTemplate.count(initialQuery, current_index);
		
	}


//	public CategoryStatResults stats(String query) {
//		
//		
//		CategoryStatResults result = new CategoryStatResults();
//		
//		final QueryStringQueryBuilder queryBuilder = new QueryStringQueryBuilder(query);
//		final NativeSearchQuery initialQuery = new NativeSearchQueryBuilder()
//				.withQuery(queryBuilder)
//				.addAggregation(AggregationBuilders.min("min_price").field("price.minPrice.price"))
//				.addAggregation(AggregationBuilders.max("max_price").field("price.minPrice.price"))
//				.addAggregation(AggregationBuilders.terms("attributes").field("attributes.unmapedAttributes.name.keyword").size(1000))				
//				.addAggregation(AggregationBuilders.terms("features").field("attributes.features.name.keyword").size(1000))				
//				.build();		
//		
//		
//	
//
//		AggregationsContainer<?> aggregations = elasticsearchTemplate.search(initialQuery, AggregatedData.class,  current_index).getAggregations();
//
//		
//		 minPrice = aggregations.get("min_price");
//		Max maxPrice = aggregations.get("max_price");
//		Terms attrs =  aggregations.get("attributes");
//		Terms features=  aggregations.get("features");
//
//		return result;
//	}

	
	/**
	 * 
	 * @return RangeQueryBuilder representing the valid dates
	 */
	private RangeQueryBuilder getValidDateQuery() {
		return QueryBuilders.rangeQuery("price.minPrice.timeStamp") 
				.gt(expirationClause());
	}

	
	/**
	 * 
	 * @return the date from when aggregateddatas are considered to be valid one
	 */
	public long expirationClause() {

		return System.currentTimeMillis() - VALID_UNTIL_DURATION;
	}
	
}
