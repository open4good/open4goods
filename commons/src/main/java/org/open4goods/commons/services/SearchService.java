package org.open4goods.commons.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.model.dto.NumericBucket;
import org.open4goods.commons.model.dto.NumericRangeFilter;
import org.open4goods.commons.model.dto.VerticalFilterTerm;
import org.open4goods.commons.model.dto.VerticalSearchRequest;
import org.open4goods.commons.model.dto.VerticalSearchResponse;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.SubsetCriteria;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.CriteriaQueryBuilder;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.HistogramAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.HistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.MaxAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.MinAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.MissingAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.mapping.FieldType;

/**
 * Service in charge of the search in Products
 *
 *
 * @author Goulven.Furet
 *
 */
public class SearchService {

	private static final int AGGREGATION_BUCKET_SIZE = 100;

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchService.class);

	public static final String MISSING_BUCKET = "ES-UNKNOWN";

	private ProductRepository aggregatedDataRepository;

	public SearchService(ProductRepository aggregatedDataRepository, String logsFolder) {
		this.aggregatedDataRepository = aggregatedDataRepository;
	}

	/**
	 * Operates a global search
	 * @param pageNumber
	 * @param pageSize
	 * @param query
	 * @return
	 */
	@Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public VerticalSearchResponse globalSearch(String initialQuery, Integer fromPrice, Integer toPrice, Set<String> categories, ProductCondition condition, int from, int to, int minOffers, boolean sort) {

		String query =  sanitize(initialQuery);


		// Logging
		LOGGER.info("global search : {}",initialQuery);

		Criteria c = null;
		if (StringUtils.isNumeric(query)) {
			// Showing even if no offers when by GTIN
			c = new Criteria("id").is(initialQuery);
		}
		else {
			// TODO(p1,security) : sanitize, web input !!
			c = 	new Criteria("offerNames").matchesAll(Arrays.asList(query.split(" ")))
					.and(aggregatedDataRepository.getRecentPriceQuery())
					;

			// NOTE : could add
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


		CriteriaQueryBuilder esQuery = new CriteriaQueryBuilder(c)
				//		.withQuery(new CriteriaQuery(c))
				.withPageable(PageRequest.of(from, to))
				.withSort(Sort.by(org.springframework.data.domain.Sort.Order.desc("offersCount")));

		SearchHits<Product> results = aggregatedDataRepository.search(esQuery.build(),ProductRepository.MAIN_INDEX_NAME);
		VerticalSearchResponse vsr = new VerticalSearchResponse();

		//		// Setting the response
		vsr.setTotalResults(results.getTotalHits());
		vsr.setFrom(from);
		vsr.setTo(to);
		vsr.setData(results.get().map(SearchHit::getContent).toList());

		return vsr;
	}

	
	/**
	 * Advanced search in a vertical
	 * @param vertical
	 * @param request
	 * @return
	 */
	// @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	// TODO(p1, performance) : put back,but collision hash with verticalsubsets (i guess)
	public VerticalSearchResponse verticalSearch(VerticalConfig vertical, VerticalSearchRequest request) {

		VerticalSearchResponse vsr = new VerticalSearchResponse();

		List<AttributeConfig> customAttrFilters = vertical.verticalFilters().stream().filter(Objects::nonNull).toList();

		Criteria criterias = new Criteria("vertical").is(vertical.getId())
				.and(aggregatedDataRepository.getRecentPriceQuery())
				;
		
		// Filtering on brand if in this disposition
		if (request.getBrandsSubset() != null) {
			if (request.getBrandsSubset().getCriterias().size() == 1) {
				SubsetCriteria cr = request.getBrandsSubset().getCriterias().getFirst();
				criterias.and(new Criteria(cr.getField()).is(cr.getValue()));
			}
		}
		
		// Adding the filter on excluded if set
		if (request.getExcludedFilters().size()>0) {
			criterias.and(new Criteria("excludedCauses").in(request.getExcludedFilters()) );	
		} else 	if (!request.isExcluded()) {
			criterias.and(new Criteria("excluded").is(false));
		}
		

		// Adding custom numeric filters
//		Criteria optional
		for (NumericRangeFilter filter : request.getNumericFilters()) {

		    if (!filter.isAllowEmptyValues()) {
		        // Strict "and" filtering for the range
		        Criteria rangeCriteria = new Criteria(filter.getKey())
		            .greaterThanEqual(filter.getMinValue())
		            .lessThanEqual(filter.getMaxValue());

		        criterias.and(rangeCriteria);
		    } else {
		        // Allow items where the field is not present OR within the range
		    	criterias.subCriteria(new Criteria().or(filter.getKey()).exists().not()
		    			.or(filter.getKey())
			            .greaterThanEqual(filter.getMinValue())
			            .lessThanEqual(filter.getMaxValue()));
		    }
		}


		// Adding custom checkbox filters
		for (Entry<String, Set<String>> filter : request.getTermsFilter().entrySet()) {
			
			// If we must show the missing, and if several clauses, we need a OR condition
			if (filter.getValue().size() > 1 &&  filter.getValue().contains(MISSING_BUCKET)) {
		    	criterias.subCriteria(new Criteria().or(filter.getKey()).exists().not()
		    			.or(filter.getKey()).in(filter.getValue()))  ;
			} else {
				if (filter.getValue().contains(MISSING_BUCKET)) {
					criterias.and(new Criteria(filter.getKey()).exists().not());					
				} else {
					
//					// TODO : dirty hack
//					if ("price.trend".equals(filter.getKey())) {
//						criterias.and(new Criteria(filter.getKey()).is((filter.getValue().stream().findAny().orElse(null))));
//					} else {
						criterias.and(new Criteria(filter.getKey()).in(filter.getValue()) );
//					}
				}
				
			}
			
		}

		/////////////////////////////////
		// Adding subset hard filtering
		/////////////////////////////////
		
		request.getSubsets().forEach(subset -> {
			subset.getCriterias().forEach(criteria -> {
				switch (criteria.getOperator()) {
				case LOWER_THAN: {
					criterias.and(new Criteria(criteria.getField()).lessThanEqual(Double.valueOf(criteria.getValue())));
					break;
				}
				case GREATER_THAN: {
					criterias.and(new Criteria(criteria.getField()).greaterThanEqual(Double.valueOf( criteria.getValue())));					
					break;
				}
				case EQUALS: {
					criterias.and(new Criteria(criteria.getField()).is(criteria.getValue()));

					break;
				}
				
				
				
				default:
					throw new IllegalArgumentException("Unexpected value: " + criteria.getOperator());
				}
				
			});
		});
		
		
		
		// Setting the query
		NativeQueryBuilder esQuery = new NativeQueryBuilder().withQuery(new CriteriaQuery(criterias));

		// Pagination
		if (null != request.getPageNumber() && null != request.getPageSize()) {
			esQuery = esQuery .withPageable(PageRequest.of(request.getPageNumber(), request.getPageSize()));
		} else {
			//TODO(p3,conf) : default pageNumber from conf
			esQuery = esQuery .withPageable(PageRequest.of(0, 100));
		}


		// Adding standard aggregations
		esQuery = esQuery
//				.withAggregation("min_price", 	Aggregation.of(a -> a.min(ta -> ta.field("price.minPrice.price"))))
//				.withAggregation("max_price", 	Aggregation.of(a -> a.max(ta -> ta.field("price.minPrice.price"))))
//				.withAggregation("min_offers", 	Aggregation.of(a -> a.min(ta -> ta.field("offersCount"))))
//				.withAggregation("max_offers", 	Aggregation.of(a -> a.max(ta -> ta.field("offersCount"))))
				.withAggregation("conditions", 	Aggregation.of(a -> a.terms(ta -> ta.field("price.conditions").missing(MISSING_BUCKET).size(3)))	)
				.withAggregation("trend", 	Aggregation.of(a -> a.terms(ta -> ta.field("price.trend").size(3)))	)
				.withAggregation("excludedCauses", 	Aggregation.of(a -> a.terms(ta -> ta.field("excludedCauses").size(20)))	)				
				.withAggregation("brands", 	Aggregation.of(a -> a.terms(ta -> ta.field("attributes.referentielAttributes.BRAND").missing(MISSING_BUCKET).size(AGGREGATION_BUCKET_SIZE)  ))	)
				.withAggregation("country", 	Aggregation.of(a -> a.terms(ta -> ta.field("gtinInfos.country").missing(MISSING_BUCKET).size(AGGREGATION_BUCKET_SIZE)  ))	)
				
				
				;
		////
		// Sort order
		/////

		if (request.getSortField() != null) {
		    SortOrder order = request.getSortOrder().equalsIgnoreCase("DESC") ? SortOrder.Desc : SortOrder.Asc;

		    SortOptions sortOptions = new SortOptions.Builder()
		        .field(new FieldSort.Builder()
		            .field(request.getSortField())
		            .order(order)
		            .unmappedType(FieldType.Float) // or "keyword"/"long" depending on the field
		            .missing("_last")
		            .build())
		        .build();

		    esQuery = esQuery.withSort(sortOptions);
		}

		// Adding custom attributes terms filters aggregations
		for (AttributeConfig attrConfig : customAttrFilters) {
			esQuery = esQuery
					.withAggregation(attrConfig.getKey(), 	Aggregation.of(a -> a.terms(ta -> ta.field("attributes.indexed."+attrConfig.getKey()+".value").missing(MISSING_BUCKET).size(AGGREGATION_BUCKET_SIZE)  ))	);
		}
		
		// Adding custom range aggregations
		for (NumericRangeFilter filter: request.getNumericFilters()) {
			esQuery = esQuery
			.withAggregation("min_"+filter.getKey(),  	Aggregation.of(a -> a.min(ta -> ta.field(filter.getKey()))))
			.withAggregation("max_"+filter.getKey(), 	Aggregation.of(a -> a.max(ta -> ta.field(filter.getKey()))))
			.withAggregation("missing_"+filter.getKey(), 	Aggregation.of(a -> a.missing(ta -> ta.field(filter.getKey()))))
			.withAggregation("interval_"+filter.getKey(), 
				    Aggregation.of(a -> a.histogram(h -> h.field(filter.getKey())
				            .interval(filter.getIntervalSize()) 
				            .minDocCount(1)) // Ensure empty buckets are excluded
				        ))
//			 .withAggregation("range_" + filter.getKey(),
//				        Aggregation.of(a -> a.range(r -> r
//				            .field(filter.getKey())
//				            .ranges(
//				            	// TODO : From conf
//				                // Fine-grained range for lower values
//				                AggregationRange.of(r1 -> r1.key("0-500").from("0").to("500")),
//				                AggregationRange.of(r2 -> r2.key("500-1500").from("500").to("1500")),
//				                // Medium steps for mid-range values
//				                AggregationRange.of(r3 -> r3.key("1500-5000").from("1500").to("5000")),
//				                // Larger steps for higher values
//				                AggregationRange.of(r4 -> r4.key("5000-10000").from("5000").to("10000")),
//				                AggregationRange.of(r5 -> r5.key("10000+").from("10000")
//				                )
//				            )
//				        ))
//				    )
			;		
		}
				
		SearchHits<Product> results;
		try {
			results = aggregatedDataRepository.search(esQuery.build(),ProductRepository.MAIN_INDEX_NAME);
		} catch (Exception e) {
			
			if (e instanceof UncategorizedElasticsearchException) {
			    Throwable cause = e.getCause();
			    if (cause instanceof ElasticsearchException ee) {
			        LOGGER.error ("Elasticsearch error: " + ee.response());
			    }
			}

			throw e;
		}


		// Handling aggregations results if relevant
		//NOTE(gof) : this cast is not nice...
		ElasticsearchAggregations aggregations = (ElasticsearchAggregations)results.getAggregations();


		///////
		// Numeric aggregations
		///////
		MinAggregate minPrice = aggregations.get("min_price.minPrice.price").aggregation().getAggregate().min();
		MaxAggregate maxPrice = aggregations.get("max_price.minPrice.price").aggregation().getAggregate().max();
		MaxAggregate maxOffers = aggregations.get("max_offersCount").aggregation().getAggregate().max();
		MinAggregate minOffers = aggregations.get("min_offersCount").aggregation().getAggregate().min();
		//
		vsr.setMaxPrice(maxPrice.value());
		vsr.setMinPrice(minPrice.value());
		vsr.setMaxOffers(Double.valueOf(maxOffers.value()).intValue());
		vsr.setMinOffers(Double.valueOf(minOffers.value()).intValue());


		
		// Adding custom range aggregations
		for (NumericRangeFilter filter: request.getNumericFilters()) {
			
			MinAggregate min = aggregations.get("min_"+filter.getKey()).aggregation().getAggregate().min();
			MaxAggregate max= aggregations.get("max_"+filter.getKey()).aggregation().getAggregate().max();
			MissingAggregate missing = aggregations.get("missing_"+filter.getKey()).aggregation().getAggregate().missing();
			HistogramAggregate priceHistogram = aggregations.get("interval_" + filter.getKey()).aggregation().getAggregate().histogram();
			
			List<NumericBucket> priceBuckets = new ArrayList<>();
			for (HistogramBucket bucket : priceHistogram.buckets().array()) {
			    priceBuckets.add(new NumericBucket(bucket.key()+"", bucket.docCount()));
			}
			
			
//			RangeAggregate priceAgg =  aggregations.get("range_" + filter.getKey()).aggregation().getAggregate().range();
//			for (RangeBucket rb : priceAgg.buckets().array()) {
//				 priceBuckets.add(new PriceBucket(rb.key()+"", rb.docCount()));
//			}
//			
			NumericRangeFilter nrf = new NumericRangeFilter();
			nrf.setMaxValue(max.value());
			nrf.setMinValue(min.value());
			nrf.setPriceBuckets(priceBuckets);
			nrf.setUnknown(missing.docCount());
			
			vsr.getNumericFilters().put(filter.getKey(), nrf);
			
		}
		
		
		///////
		// Price trend
		///////

		LongTermsAggregate productSate  =  aggregations.get("trend").aggregation().getAggregate().lterms();
		for (LongTermsBucket b :   productSate.buckets().array()) {
			
			if (b.key() == -1) {
				vsr.setPriceDecreasing(b.docCount());
			}
		}
		
		///////
		// Item condition
		///////

		StringTermsAggregate priceTrend  =  aggregations.get("conditions").aggregation().getAggregate().sterms();
		for (StringTermsBucket b :   priceTrend.buckets().array()) {
			vsr.getConditions().add (new VerticalFilterTerm(b.key().stringValue(), b.docCount()));
		}
		vsr.getConditions().sort((o1, o2) -> o2.getCount().compareTo(o1.getCount()));


		///////
		// Brands
		///////

		StringTermsAggregate brands  =  aggregations.get("brands").aggregation().getAggregate().sterms() ;
		for (StringTermsBucket b :   brands.buckets().array()) {
			vsr.getBrands().add (new VerticalFilterTerm(b.key().stringValue(), b.docCount()));
		}
		vsr.getBrands().sort((o1, o2) -> o2.getCount().compareTo(o1.getCount()));

		///////
		// Countries
		///////

		StringTermsAggregate countries  =  aggregations.get("country").aggregation().getAggregate().sterms() ;
		for (StringTermsBucket bucket : countries.buckets().array()) {
			vsr.getCountries().add (new VerticalFilterTerm(bucket.key().stringValue(), bucket.docCount()));
		}
		vsr.getCountries().sort((o1, o2) -> o2.getCount().compareTo(o1.getCount()));

		///////
		// excluded
		///////

		StringTermsAggregate excluded  =  aggregations.get("excludedCauses").aggregation().getAggregate().sterms() ;
		for (StringTermsBucket b :   excluded.buckets().array()) {
			vsr.getExcluded().add (new VerticalFilterTerm(b.key().stringValue(), b.docCount()));
		}
		vsr.getExcluded().sort((o1, o2) -> o2.getCount().compareTo(o1.getCount()));

		
		//////////////
		/// Attr filters
		//////////////


		//		// Handling custom filters aggregations
		for (AttributeConfig attrConfig : customAttrFilters) {
			StringTermsAggregate agg  =  aggregations.get(attrConfig.getKey()).aggregation().getAggregate().sterms() ;
			vsr.getCustomFilters().put(attrConfig, new ArrayList<>());
			for (StringTermsBucket bucket : agg.buckets().array()) {

				if (!bucket.key().stringValue().equals(MISSING_BUCKET)) {
					vsr.getCustomFilters().get(attrConfig).add (new VerticalFilterTerm(bucket.key().stringValue(), bucket.docCount()));
				} else {
					vsr.getCustomFilters().get(attrConfig).add (new VerticalFilterTerm(MISSING_BUCKET, bucket.docCount()));
				}
			}

			if (attrConfig.getAttributeValuesOrdering().equals(org.open4goods.model.vertical.Order.COUNT ) ) {
				vsr.getCustomFilters().get(attrConfig).sort((o1, o2) -> o2.getCount().compareTo(o1.getCount()));
			}
			else {
				vsr.getCustomFilters().get(attrConfig).sort(Comparator.comparing(VerticalFilterTerm::getText));
			}

		}

		// Setting the response
		vsr.setTotalResults(results.getTotalHits());
		vsr.setData(results.get().map(SearchHit::getContent).toList());

		vsr.setVerticalConfig(vertical);
		vsr.setRequest(request);
		String queryString = StringUtils.join(criterias.getCriteriaChain(), "\n-> ");
		LOGGER.info("Searching in vertical {} : {} results for query \n-> {}", vertical.getId(), vsr.getTotalResults(), queryString);
//		verticalstatsLogger.info("Searching in vertical {} : {}",vertical.getId(), request.toString());

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
