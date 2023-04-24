package org.open4goods.ui.services;

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
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.constants.ProductState;
import org.open4goods.model.product.AggregatedData;
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
	 * @param from 
	 * @param to 
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
		
		vsr.setVerticalName(ALL_VERTICAL_NAME);	
		
		
		return vsr;
	}

	
	
	public VerticalSearchResponse verticalSearch(VerticalConfig vertical, String initialQuery, Integer fromPrice, Integer toPrice, ProductState condition, Integer from, Integer to, int minOffersToShow, boolean sort) {
		
//		String query = initialQuery == null ? "" :  sanitize(initialQuery);
		
		// Logging
		statsLogger.info("Searching {}",initialQuery);
				
		// Valid timestamp
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()  
				.must(aggregatedDataRepository.getValidDateQuery())	
				;
		queryBuilder = queryBuilder.must(QueryBuilders.matchQuery("vertical.keyword",vertical.getId() ));

		
		// from price
		if (null != fromPrice) {
			queryBuilder = queryBuilder.must(QueryBuilders.rangeQuery("price.minPrice.price").gt(fromPrice.intValue()));			 			
		} else {
			queryBuilder = queryBuilder.must(QueryBuilders.rangeQuery("price.minPrice.price").gt(0.0));
		}
		
		// to price
		if (null != toPrice) {
			queryBuilder = queryBuilder.must(QueryBuilders.rangeQuery("price.minPrice.price").lt(toPrice.intValue()));			 			
		} else {
			queryBuilder = queryBuilder.must(QueryBuilders.rangeQuery("price.minPrice.price").lt(Integer.MAX_VALUE));	
		}
		
		// condition
		if (null != condition) {
			queryBuilder = queryBuilder.must(QueryBuilders.termQuery("price.minPrice.productState", condition.toString()));
		}
		
		// Setting the query
		NativeSearchQueryBuilder esQuery = new NativeSearchQueryBuilder()
				.withQuery(queryBuilder)

				;
		
		if (null != from && null != to) {
			esQuery = esQuery .withPageable(PageRequest.of(from, to));
		} else {
			//TODO(gof) : from conf
			esQuery = esQuery .withPageable(PageRequest.of(0, 1000));
		}
		
		// Adding standard aggregations
		esQuery = esQuery 
				.withAggregations(AggregationBuilders.min("min_price").field("price.minPrice.price"))
				.withAggregations(AggregationBuilders.max("max_price").field("price.minPrice.price"))
				.withAggregations(AggregationBuilders.max("max_offers").field("offersCount"))
				//TODO : could optimize by setting at 1
				.withAggregations(AggregationBuilders.min("min_offers").field("offersCount"))
				//TODO: store the productState at indexation, faster and no counting by offers
				.withAggregations(AggregationBuilders.terms("condition").field("price.offers.productState").size(5))	
				.withAggregations(AggregationBuilders.terms("brands").field("attributes.referentielAttributes.BRAND.keyword").size(500))	
				
				.withQuery(queryBuilder)
				.withSort(SortBuilders.fieldSort("offersCount").order(SortOrder.DESC))
//		.withSorts(SortBuilders.fieldSort("minOffers"))

		;
		
//		if (sort) {
//			 esQuery.        withSort(SortBuilders.fieldSort("offersCount").order(SortOrder.DESC));
//		}
//		
		
		SearchHits<AggregatedData> results = aggregatedDataRepository.search(esQuery.build(),ALL_VERTICAL_NAME);
		VerticalSearchResponse vsr = new VerticalSearchResponse();			

		// Handling aggregations results if relevant
		Aggregations aggregations = (Aggregations)results.getAggregations().aggregations();
					
		Min minPrice = aggregations.get("min_price");
		Max maxPrice = aggregations.get("max_price");
		Max maxOffers = aggregations.get("max_offers");
		Min minOffers = aggregations.get("min_offers");
		Terms brands  =  aggregations.get("brands");
		Terms productSate  =  aggregations.get("condition");		
		
		vsr.setMaxPrice(maxPrice.getValue());
		vsr.setMinPrice(minPrice.getValue());
		vsr.setMaxOffers(Double.valueOf(maxOffers.getValue()).intValue());
		vsr.setMinOffers(Double.valueOf(minOffers.getValue()).intValue());
		
		if (null != productSate.getBucketByKey(ProductState.NEW.toString())) {			
			vsr.setItemNew( productSate.getBucketByKey(ProductState.NEW.toString()).getDocCount()) ;
		}

		if (null != productSate.getBucketByKey(ProductState.OCCASION.toString())) {			
			vsr.setItemOccasion( productSate.getBucketByKey(ProductState.OCCASION.toString()).getDocCount()) ;
		}
		
		if (null != productSate.getBucketByKey(ProductState.UNKNOWN.toString())) {			
			vsr.setItemUnknown(productSate.getBucketByKey(ProductState.UNKNOWN.toString()).getDocCount());
		}
		
		for (Bucket b :   brands.getBuckets()) {			
			vsr.getBrands().put(b.getKey().toString(), b.getDocCount());			
		}

//		// Setting the response
		vsr.setTotalResults(results.getTotalHits());
		vsr.setFrom(from);
		vsr.setTo(to);
		vsr.setData(results.get().map(e-> e.getContent()).toList());
		
		vsr.setVerticalName(vertical.getId());	
		
		vsr.setVerticalConfig(vertical);	
		
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
		
		
		// TODO(gof) : Page from conf
		vsr.setData(aggregatedDataRepository.searchValidPrices(translatedVerticalQuery,ALL_VERTICAL_NAME,0,1000) .collect(Collectors.toList()));
		vsr.setVerticalName(ALL_VERTICAL_NAME);	
		
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
