package org.open4goods.ui.services;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.attributes.AttributeConfig;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.constants.ProductState;
import org.open4goods.model.product.AggregatedData;
import org.open4goods.ui.controllers.dto.NumericRangeFilter;
import org.open4goods.ui.controllers.dto.VerticalSearchRequest;
import org.open4goods.ui.controllers.dto.VerticalSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.client.elc.QueryBuilders;
import org.springframework.data.elasticsearch.client.erhlc.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Order;

import ch.qos.logback.classic.Level;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;

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
	// TODO(perf : cache)
	public VerticalSearchResponse globalSearch(String initialQuery, Integer fromPrice, Integer toPrice, Set<String> categories, ProductState condition, int from, int to, int minOffers, boolean sort) {
		
		String query =  sanitize(initialQuery);
		
		// Logging
		statsLogger.info("Searching {}",initialQuery);
		
		Criteria c = null;
		if (StringUtils.isNumeric(query)) {
			// Showing even if no offers when by GTIN
			c = new Criteria("attributes.referentielAttributes.GTIN.keyword").is(initialQuery);
		}
		else {
			c = aggregatedDataRepository.getValidDateQuery()
					// TODO(security) : sanitize, web imput !!
					.and(new Criteria("names.offerNames").in(Arrays.asList(query.split(" ")))
					.and("offersCount").greaterThanEqual(1)			)	


					;
			
			// TODO : could add
//			price
//			vertical
//			offerscount
//			neuf / occasion
//			
//			barcode nationality
//			garantie
//			ecoscore
//			classe energie
			
		}
	

		NativeQueryBuilder esQuery = new NativeQueryBuilder()
		.withQuery(new CriteriaQuery(c))
		.withPageable(PageRequest.of(from, to))
		.withSort(Sort.by(Order.desc("offersCount")));

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
		
		
		Criteria criterias = aggregatedDataRepository.getValidDateQuery();
		criterias.and(new Criteria("vertical.keyword").is(vertical.getId()));
		
		
		// min price
		if (null != request.getMinPrice()) {
			criterias.and(new Criteria("price.minPrice.price").greaterThanEqual(Math.floor(request.getMinPrice())));
		} 
		
		// max price
		if (null != request.getMaxPrice()) {
			criterias.and(new Criteria("price.minPrice.price").lessThanEqual(Math.ceil(request.getMaxPrice())));
		} 
		
		
		// Adding custom numeric filters		
		for (NumericRangeFilter filter : request.getNumericFilters()) {
			criterias.and(new Criteria("filter.getAttribute()").lessThanEqual(filter.getMaxValue()).greaterThanEqual(filter.getMinValue()));
	 						
		}		
		
		// Adding custom checkbox filters		
		for (Entry<String, Set<String>> filter : request.getTermsFilter().entrySet()) {
			criterias.and(new Criteria(filter.getKey()).in(filter.getValue()) );
		}
		
		// condition
		if (null != request.getCondition()) {
			criterias.and(new Criteria("price.conditions").in(request.getCondition().toString()) );
		}
		
		// min offersCount
		if (null != request.getMinOffers()) {
			criterias.and(new Criteria("offersCount").greaterThanEqual(request.getMinOffers()));			
		}
		
		// max offersCount
		if (null != request.getMaxOffers()) {
			criterias.and(new Criteria("offersCount").lessThanEqual(request.getMaxOffers()));		
		}

		
		// Setting the query
		NativeQueryBuilder esQuery = new NativeQueryBuilder().withQuery(new CriteriaQuery(criterias));
		
		if (null != request.getPageNumber() && null != request.getPageSize()) {
			esQuery = esQuery .withPageable(PageRequest.of(request.getPageNumber(), request.getPageSize()));
		} else {
			//TODO(gof) : pageNumber conf
			esQuery = esQuery .withPageable(PageRequest.of(0, 100));
		}
		
		
		// Adding standard aggregations
		esQuery = esQuery 
				.withAggregation("min_price", 	Aggregation.of(a -> a.min(ta -> ta.field("price.minPrice.price"))))
				.withAggregation("max_price", 	Aggregation.of(a -> a.max(ta -> ta.field("price.minPrice.price"))))

				.withAggregation("min_offers", 	Aggregation.of(a -> a.min(ta -> ta.field("offersCount"))))
				.withAggregation("max_offers", 	Aggregation.of(a -> a.max(ta -> ta.field("offersCount"))))

				.withAggregation("condition", 	Aggregation.of(a -> a.terms(ta -> ta.field("price.conditions").missing(OTHER_BUCKET).size(3)  ))	)
				
				// TODO : size from conf
				.withAggregation("brands", 	Aggregation.of(a -> a.terms(ta -> ta.field("attributes.referentielAttributes.BRAND.keyword").missing(OTHER_BUCKET).size(100)  ))	)
				
				// TODO : size from conf
				.withAggregation("country", 	Aggregation.of(a -> a.terms(ta -> ta.field("gtinInfos.country").missing(OTHER_BUCKET).size(100)  ))	)
						
				.withQuery(new CriteriaQuery(criterias));
		
		// Sort order
		
		if (null == request.getSortField()) {
			esQuery = esQuery.withSort(Sort.by(Order.desc("offersCount")));
		} else {
			// TODO : check value, remove ignorecase
			if (request.getSortOrder().equalsIgnoreCase("DESC") ) {
				esQuery = esQuery.withSort(Sort.by(Order.desc(request.getSortField())));
			} else if (request.getSortOrder().equalsIgnoreCase("ASC") ){
				esQuery = esQuery.withSort(Sort.by(Order.asc(request.getSortField())));
			} else {
				throw new RuntimeException("implement");
			}
		}
		
		// Adding custom filters aggregations	
		for (AttributeConfig attrConfig : customAttrFilters) {
			esQuery = esQuery 
					// TODO : size from conf
					.withAggregation(attrConfig.getKey(), 	Aggregation.of(a -> a.terms(ta -> ta.field("attributes.aggregatedAttributes."+attrConfig.getKey()+".value.keyword").missing(OTHER_BUCKET).size(100)  ))	);			
		}
		

		SearchHits<AggregatedData> results = aggregatedDataRepository.search(esQuery.build(),ALL_VERTICAL_NAME);
	

		// Handling aggregations results if relevant
		AggregationsContainer<?> aggregations = results.getAggregations();
					
//		Min minPrice = aggregations. get("min_price");
//		Max maxPrice = aggregations.get("max_price");
//		Max maxOffers = aggregations.get("max_offers");
//		Min minOffers = aggregations.get("min_offers");
//		Terms brands  =  aggregations.get("brands");
//		Terms productSate  =  aggregations.get("condition");		
//		Terms countries  =  aggregations.get("country");		
//		
//		vsr.setMaxPrice(maxPrice.getValue());
//		vsr.setMinPrice(minPrice.getValue());
//		vsr.setMaxOffers(Double.valueOf(maxOffers.getValue()).intValue());
//		vsr.setMinOffers(Double.valueOf(minOffers.getValue()).intValue());
//		
//
//
//		for (Bucket b :   productSate.getBuckets()) {			
//			vsr.getConditions().add (new VerticalFilterTerm(b.getKey().toString(), b.getDocCount()));			
//		}
//		vsr.getConditions().sort((o1, o2) -> o2.getCount().compareTo(o1.getCount()));
//		// Adding others
//		if (null != productSate.getBucketByKey(OTHER_BUCKET)) {		
//			vsr.getConditions().add ( new VerticalFilterTerm(OTHER_BUCKET,productSate.getBucketByKey(OTHER_BUCKET).getDocCount()));
//		}
//		
//		for (Bucket b :   brands.getBuckets()) {			
//			vsr.getBrands().add (new VerticalFilterTerm(b.getKey().toString(), b.getDocCount()));			
//		}
//		vsr.getBrands().sort((o1, o2) -> o2.getCount().compareTo(o1.getCount()));
//		// Adding others
//		if (null != brands.getBucketByKey(OTHER_BUCKET)) {		
//			vsr.getBrands().add ( new VerticalFilterTerm(OTHER_BUCKET,brands.getBucketByKey(OTHER_BUCKET).getDocCount()));
//		}
//		
//		for (Bucket bucket : countries.getBuckets()) {
//			vsr.getCountries().add (new VerticalFilterTerm(bucket.getKey().toString(), bucket.getDocCount()));
//		}
//		vsr.getCountries().sort((o1, o2) -> o2.getCount().compareTo(o1.getCount()));
//		// Adding others
//		if (null != countries.getBucketByKey(OTHER_BUCKET)) {
//			vsr.getCountries().add ( new VerticalFilterTerm(OTHER_BUCKET,countries.getBucketByKey(OTHER_BUCKET).getDocCount()));
//		}
//		
//		// Handling custom filters aggregations	
//		for (AttributeConfig attrConfig : customAttrFilters) {
//			Terms agg  =  aggregations.get(attrConfig.getKey());
//			vsr.getCustomFilters().put(attrConfig, new ArrayList<>());
//			for (Bucket bucket : agg.getBuckets()) {
//				
//				if (!bucket.getKey().toString().equals(OTHER_BUCKET)) {					
//					vsr.getCustomFilters().get(attrConfig).add (new VerticalFilterTerm(bucket.getKey().toString(), bucket.getDocCount()));
//				}
//			}
//			
//			if (attrConfig.getAttributeValuesOrdering().equals(org.open4goods.config.yml.attributes.Order.COUNT ) ) {
//				vsr.getCustomFilters().get(attrConfig).sort((o1, o2) -> o2.getCount().compareTo(o1.getCount()));				
//			}
//			else {
//				vsr.getCustomFilters().get(attrConfig).sort((o1, o2) -> o1.getText().compareTo(o2.getText()));								
//			}
//			
//			// Adding others
//			if (null != agg.getBucketByKey(OTHER_BUCKET)) {
//				vsr.getCustomFilters().get(attrConfig).add ( new VerticalFilterTerm(OTHER_BUCKET,agg.getBucketByKey(OTHER_BUCKET).getDocCount()));
//			}
//		}		

//		// Setting the response
		vsr.setTotalResults(results.getTotalHits());
		vsr.setData(results.get().map(e-> e.getContent()).toList());
				
		vsr.setVerticalConfig(vertical);	
		vsr.setRequest(request);
		
		return vsr;
	}

	
//	/**
//	 * 
//	 * @param categorie
//	 * @return all AggregatedData for a categorie
//	 */
//	public VerticalSearchResponse categorieSearch(String categorie) {
//		String translatedVerticalQuery="datasourceCategories:\""+categorie+"\""; 
//		
//		
//		
//		VerticalSearchResponse vsr = new VerticalSearchResponse();
//		
//		//TODO : Handle attributes and limits
////		 CategoryStatResults statsResult = aggregatedDataRepository.stats(translatedVerticalQuery);
//		
//		
//		// TODO(gof) : Page pageNumber conf
//		vsr.setData(aggregatedDataRepository.searchValidPrices(translatedVerticalQuery,ALL_VERTICAL_NAME,0,1000) .collect(Collectors.toList()));
//		
//		return vsr;
//	}
//	
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
