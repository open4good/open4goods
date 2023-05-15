package org.open4goods.ui.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.metrics.Max;
import org.elasticsearch.search.aggregations.metrics.Min;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.open4goods.config.yml.attributes.AttributeConfig;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.constants.ProductState;
import org.open4goods.model.product.AggregatedData;
import org.open4goods.ui.controllers.dto.NumericRangeFilter;
import org.open4goods.ui.controllers.dto.VerticalFilterTerm;
import org.open4goods.ui.controllers.dto.VerticalSearchRequest;
import org.open4goods.ui.controllers.dto.VerticalSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import ch.qos.logback.classic.Level;

/**
 * Service in charge of the search in AggregatedDatas and in DataFragments
 *
 * 
 * @author Goulven.Furet
 *
 */
public class SearchService {

	private static final String OTHER_BUCKET = "UNKNOWN";


	private static final Logger LOGGER = LoggerFactory.getLogger(SearchService.class);


	static final String ALL_VERTICAL_NAME = "all";

	
	private AggregatedDataRepository aggregatedDataRepository;

	private Logger statsLogger;
	
	
	@Autowired
	public SearchService(AggregatedDataRepository aggregatedDataRepository, String logsFolder) {
		this.aggregatedDataRepository = aggregatedDataRepository;
		this.statsLogger  = GenericFileLogger.initLogger("stats-search", Level.INFO, logsFolder, false);
	}
	
	/**
	 * Operates a search on each vertical, and on datafragments if no results in verticals
	 * TODO(P1,security,0.75) : enable results limitation
	 * @param pageNumber 
	 * @param pageSize 
	 * @param query
	 * @return
	 */
	public VerticalSearchResponse globalSearch(String initialQuery, Integer fromPrice, Integer toPrice, Set<String> categories, ProductState condition, int from, int to, int minOffers, boolean sort) {
		
		String query =  sanitize(initialQuery);
		
		// Logging
		statsLogger.info("Searching {}",initialQuery);
		
		
		// Valid timestamp
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()  
				.must(aggregatedDataRepository.getValidDateQuery())				
				;


		// Query string query
		if (!StringUtils.isEmpty(query)) {			
			String frags[] = query.split(" ");
			String q = Stream.of(frags)
					.map(e -> "names.offerNames:"+e)				
					. collect(Collectors.joining(" AND "));

			String translatedVerticalQuery ="attributes.referentielAttributes.GTIN.keyword:\""+query+"\"  OR ("+q+")"; 		
			
			// Adding minoffers filter
			if (minOffers > 0) {
				translatedVerticalQuery += " AND offersCount:> " + minOffers;
				
			}
			
			
			queryBuilder = queryBuilder.must(new QueryStringQueryBuilder(translatedVerticalQuery));		
		}
	
		NativeSearchQueryBuilder esQuery = new NativeSearchQueryBuilder()
		.withQuery(queryBuilder)
		.withPageable(PageRequest.of(from, to))
		.withSort(SortBuilders.fieldSort("offersCount").order(SortOrder.DESC))
//		.withSorts(SortBuilders.fieldSort("minOffers"))

		;
		
//		if (sort) {
//			 esQuery.        withSort(SortBuilders.fieldSort("offersCount").order(SortOrder.DESC));
//		}
//		
		
		
		
		SearchHits<AggregatedData> results = aggregatedDataRepository.search(esQuery.build(),ALL_VERTICAL_NAME);
		VerticalSearchResponse vsr = new VerticalSearchResponse();			

//		// Setting the response
		vsr.setTotalResults(results.getTotalHits());
		vsr.setFrom(from);
		vsr.setTo(to);
		vsr.setData(results.get().map(e-> e.getContent()).toList());
		
		return vsr;
	}

	

	/**
	 * Advanced search in a vertical
	 * @param vertical
	 * @param request
	 * @return
	 */
	public VerticalSearchResponse verticalSearch(VerticalConfig vertical, VerticalSearchRequest request) {
		
		// Logging
		statsLogger.info("Searching {}",request);
				
		VerticalSearchResponse vsr = new VerticalSearchResponse();	
		
		List<AttributeConfig> customAttrFilters = vertical.verticalFilters();
		
		// Valid timestamp
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()  
				.must(QueryBuilders.matchQuery("vertical.keyword",vertical.getId() ))
				.must(aggregatedDataRepository.getValidDateQuery())	
				;
		
		// min price
		if (null != request.getMinPrice()) {
			queryBuilder = queryBuilder.must(QueryBuilders.rangeQuery("price.minPrice.price").gte(Math.floor(request.getMinPrice())));			 			
		} else {
			//TODO : test removing
//			queryBuilder = queryBuilder.must(QueryBuilders.rangeQuery("price.minPrice.price").gt(0.0));
		}
		
		// max price
		if (null != request.getMaxPrice()) {
			queryBuilder = queryBuilder.must(QueryBuilders.rangeQuery("price.minPrice.price").lte(Math.ceil(request.getMaxPrice())));			 			
		} else {
			//TODO : test removing
//			queryBuilder = queryBuilder.must(QueryBuilders.rangeQuery("price.minPrice.price").lt(Integer.MAX_VALUE));	
		}
		
		
		// Adding custom numeric filters		
		for (NumericRangeFilter filter : request.getNumericFilters()) {
			queryBuilder = queryBuilder.must(QueryBuilders.rangeQuery(filter.getAttribute())
											.lte(filter.getMaxValue())
											.gte(filter.getMinValue()));			 						
		}
		
		// condition
		if (null != request.getCondition()) {
			queryBuilder = queryBuilder.must(QueryBuilders.termQuery("price.minPrice.productState", request.getCondition().toString()));
		}
		
		
		
		// min offersCount
		if (null != request.getMinOffers()) {
			queryBuilder = queryBuilder.must(QueryBuilders.rangeQuery("offersCount").gte(request.getMinOffers()));
		}
		
		// max offersCount
		if (null != request.getMaxOffers()) {
			queryBuilder = queryBuilder.must(QueryBuilders.rangeQuery("offersCount").lte(request.getMaxOffers()+1));
		}
		
		
		// Setting the query
		NativeSearchQueryBuilder esQuery = new NativeSearchQueryBuilder().withQuery(queryBuilder);
		
		if (null != request.getPageNumber() && null != request.getPageSize()) {
			esQuery = esQuery .withPageable(PageRequest.of(request.getPageNumber(), request.getPageSize()));
		} else {
			//TODO(gof) : pageNumber conf
			esQuery = esQuery .withPageable(PageRequest.of(0, 100));
		}
		
		
		
		
		
		
		// Adding standard aggregations
		esQuery = esQuery 
				.withAggregations(AggregationBuilders.min("min_price").field("price.minPrice.price"))
				.withAggregations(AggregationBuilders.max("max_price").field("price.minPrice.price"))
				.withAggregations(AggregationBuilders.max("max_offers").field("offersCount"))
				//TODO : could optimize by setting at 1
				.withAggregations(AggregationBuilders.min("min_offers").field("offersCount"))
				//TODO: store the productState at indexation, faster and no counting by offers
				.withAggregations(AggregationBuilders.terms("condition").field("price.offers.productState").missing(OTHER_BUCKET).size(4))	
				.withAggregations(AggregationBuilders.terms("brands").field("attributes.referentielAttributes.BRAND.keyword").missing(OTHER_BUCKET).size(500))	
				.withAggregations(AggregationBuilders.terms("country").field("gtinInfos.country").missing(OTHER_BUCKET).size(500))	
				.withQuery(queryBuilder);
		
		// Sort order
		
		if (null == request.getSortField()) {
			esQuery = esQuery.withSort(SortBuilders.fieldSort("offersCount").order(SortOrder.DESC));
		} else {
			esQuery = esQuery.withSort(SortBuilders.fieldSort(request.getSortField()).order(request.getSortOrder()));
		}


		
		// Adding custom filters aggregations	
		for (AttributeConfig attrConfig : customAttrFilters) {
			esQuery = esQuery 
					.withAggregations(AggregationBuilders.terms(attrConfig.getKey()).field("attributes.aggregatedAttributes."+attrConfig.getKey()+".value.keyword").missing(OTHER_BUCKET) );			
		}
		

		SearchHits<AggregatedData> results = aggregatedDataRepository.search(esQuery.build(),ALL_VERTICAL_NAME);
	

		// Handling aggregations results if relevant
		Aggregations aggregations = (Aggregations)results.getAggregations().aggregations();
					
		Min minPrice = aggregations.get("min_price");
		Max maxPrice = aggregations.get("max_price");
		Max maxOffers = aggregations.get("max_offers");
		Min minOffers = aggregations.get("min_offers");
		Terms brands  =  aggregations.get("brands");
		Terms productSate  =  aggregations.get("condition");		
		Terms countries  =  aggregations.get("country");		
		
		vsr.setMaxPrice(maxPrice.getValue());
		vsr.setMinPrice(minPrice.getValue());
		vsr.setMaxOffers(Double.valueOf(maxOffers.getValue()).intValue());
		vsr.setMinOffers(Double.valueOf(minOffers.getValue()).intValue());
		


		for (Bucket b :   productSate.getBuckets()) {			
			vsr.getConditions().add (new VerticalFilterTerm(b.getKey().toString(), b.getDocCount()));			
		}
		vsr.getConditions().sort((o1, o2) -> o2.getCount().compareTo(o1.getCount()));
		// Adding others
		if (null != productSate.getBucketByKey(OTHER_BUCKET)) {		
			vsr.getConditions().add ( new VerticalFilterTerm(OTHER_BUCKET,productSate.getBucketByKey(OTHER_BUCKET).getDocCount()));
		}
		
		for (Bucket b :   brands.getBuckets()) {			
			vsr.getBrands().add (new VerticalFilterTerm(b.getKey().toString(), b.getDocCount()));			
		}
		vsr.getBrands().sort((o1, o2) -> o2.getCount().compareTo(o1.getCount()));
		// Adding others
		if (null != brands.getBucketByKey(OTHER_BUCKET)) {		
			vsr.getBrands().add ( new VerticalFilterTerm(OTHER_BUCKET,brands.getBucketByKey(OTHER_BUCKET).getDocCount()));
		}
		
		for (Bucket bucket : countries.getBuckets()) {
			vsr.getCountries().add (new VerticalFilterTerm(bucket.getKey().toString(), bucket.getDocCount()));
		}
		vsr.getCountries().sort((o1, o2) -> o2.getCount().compareTo(o1.getCount()));
		// Adding others
		if (null != countries.getBucketByKey(OTHER_BUCKET)) {
			vsr.getCountries().add ( new VerticalFilterTerm(OTHER_BUCKET,countries.getBucketByKey(OTHER_BUCKET).getDocCount()));
		}
		
		// Handling custom filters aggregations	
		for (AttributeConfig attrConfig : customAttrFilters) {
			Terms agg  =  aggregations.get(attrConfig.getKey());
			vsr.getCustomFilters().put(attrConfig, new ArrayList<>());
			for (Bucket bucket : agg.getBuckets()) {
				
				if (!bucket.getKey().toString().equals(OTHER_BUCKET)) {					
					vsr.getCustomFilters().get(attrConfig).add (new VerticalFilterTerm(bucket.getKey().toString(), bucket.getDocCount()));
				}
			}
			
			if (attrConfig.getAttributeValuesOrdering().equals(org.open4goods.config.yml.attributes.Order.COUNT ) ) {
				vsr.getCustomFilters().get(attrConfig).sort((o1, o2) -> o2.getCount().compareTo(o1.getCount()));				
			}
			else {
				vsr.getCustomFilters().get(attrConfig).sort((o1, o2) -> o1.getText().compareTo(o2.getText()));								
			}
			
			// Adding others
			if (null != agg.getBucketByKey(OTHER_BUCKET)) {
				vsr.getCustomFilters().get(attrConfig).add ( new VerticalFilterTerm(OTHER_BUCKET,agg.getBucketByKey(OTHER_BUCKET).getDocCount()));
			}
		}		

//		// Setting the response
		vsr.setTotalResults(results.getTotalHits());
		vsr.setData(results.get().map(e-> e.getContent()).toList());
				
		vsr.setVerticalConfig(vertical);	
		vsr.setRequest(request);
		
		return vsr;
	}

	
	/**
	 * 
	 * @param categorie
	 * @return all AggregatedData for a categorie
	 */
	public VerticalSearchResponse categorieSearch(String categorie) {
		String translatedVerticalQuery="datasourceCategories:\""+categorie+"\""; 
		
		
		
		VerticalSearchResponse vsr = new VerticalSearchResponse();
		
		//TODO : Handle attributes and limits
//		 CategoryStatResults statsResult = aggregatedDataRepository.stats(translatedVerticalQuery);
		
		
		// TODO(gof) : Page pageNumber conf
		vsr.setData(aggregatedDataRepository.searchValidPrices(translatedVerticalQuery,ALL_VERTICAL_NAME,0,1000) .collect(Collectors.toList()));
		
		return vsr;
	}
	
	public String sanitize(String q) {
		return StringUtils.normalizeSpace(q.replace("(", " ")
				.replace(")", " ")
				.replace("[", " ")
				.replace("]", " ")
				.replace("+", " ")
				.replace("-", " ")
				.replace(":", " ")
				.replace("\"", " "));
	
	}


}
