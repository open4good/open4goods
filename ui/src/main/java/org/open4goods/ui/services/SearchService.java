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
	public VerticalSearchResponse globalSearch(String initialQuery, Integer fromPrice, Integer toPrice, Set<String> categories, ProductState condition, int from, int to) {
		
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
			queryBuilder = queryBuilder.must(new QueryStringQueryBuilder(translatedVerticalQuery));		
		}
		
		// from price
		if (null != fromPrice) {
			queryBuilder = queryBuilder.must(QueryBuilders.rangeQuery("price.minPrice.price").gt(fromPrice.intValue()));			 			
		}
		
		// to price
		if (null != toPrice) {
			queryBuilder = queryBuilder.must(QueryBuilders.rangeQuery("price.minPrice.price").lt(toPrice.intValue()));			 			
		}
		
		// condition
		if (null != condition) {
			queryBuilder = queryBuilder.must(QueryBuilders.termQuery("price.minPrice.productState", condition.toString()));
		}
		
		// Categories
		if (null != categories && categories.size() > 0) {
			queryBuilder = queryBuilder.must(QueryBuilders.termsQuery("datasourceCategories", categories));
		}
		
	
		// Setting the query
		NativeSearchQueryBuilder esQuery = new NativeSearchQueryBuilder()
				.withQuery(queryBuilder)
				.withPageable(PageRequest.of(from, to))
				;
		
		// Adding aggregations
		esQuery = esQuery 
				.withAggregations(AggregationBuilders.min("min_price").field("price.minPrice.price"))
				.withAggregations(AggregationBuilders.max("max_price").field("price.minPrice.price"))
				.withAggregations(AggregationBuilders.max("max_offers").field("offersCount"))				
				.withAggregations(AggregationBuilders.min("min_offers").field("offersCount"))				
				.withAggregations(AggregationBuilders.terms("condition").field("price.minPrice.productState").size(5))	
				.withAggregations(AggregationBuilders.terms("categories").field("datasourceCategories").size(5000))	
				
				
				;		
		
				
		SearchHits<AggregatedData> results = aggregatedDataRepository.search(esQuery.build(),ALL_VERTICAL_NAME,from,to);

		VerticalSearchResponse vsr = new VerticalSearchResponse();			

		// Handling aggregations results if relevant
		Aggregations aggregations = (Aggregations)results.getAggregations().aggregations();
					
		Min minPrice = aggregations.get("min_price");
		Max maxPrice = aggregations.get("max_price");
		Max maxOffers = aggregations.get("max_offers");
		Min minOffers = aggregations.get("min_offers");
		
		Terms productSate  =  aggregations.get("condition");
		Terms catTerms  =  aggregations.get("categories");
		
		
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
		
		for (Bucket b :   catTerms.getBuckets()) {			
			vsr.getCategories().put(b.getKey().toString(), b.getDocCount());			
		}
		
		// Setting the response
		vsr.setTotalResults(results.getTotalHits());
		vsr.setFrom(from);
		vsr.setTo(to);
		vsr.setData(results.get().map(e-> e.getContent()).toList());
		
		vsr.setVerticalName(ALL_VERTICAL_NAME);	
		
		
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
