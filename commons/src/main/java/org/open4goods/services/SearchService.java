package org.open4goods.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.attributes.AttributeConfig;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.constants.ProductCondition;
import org.open4goods.model.dto.NumericRangeFilter;
import org.open4goods.model.dto.VerticalFilterTerm;
import org.open4goods.model.dto.VerticalSearchRequest;
import org.open4goods.model.dto.VerticalSearchResponse;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.CriteriaQueryBuilder;

import ch.qos.logback.classic.Level;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.MaxAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.MinAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;

/**
 * Service in charge of the search in AggregatedDatas and in DataFragments
 *
 *
 * @author Goulven.Furet
 *
 */
public class SearchService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchService.class);



	public static final String OTHER_BUCKET = "ES-UNKNOWN";


	private ProductRepository aggregatedDataRepository;


	// Dedicated loggers, to get some stats
	private Logger verticalstatsLogger;
	private Logger globalstatsLogger;


	public SearchService(ProductRepository aggregatedDataRepository, String logsFolder) {
		this.aggregatedDataRepository = aggregatedDataRepository;
		globalstatsLogger  = GenericFileLogger.initLogger("stats-search-global", Level.INFO, logsFolder, false);
		verticalstatsLogger  = GenericFileLogger.initLogger("stats-search-vertical", Level.INFO, logsFolder, false);
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
	public VerticalSearchResponse globalSearch(String initialQuery, Integer fromPrice, Integer toPrice, Set<String> categories, ProductCondition condition, int from, int to, int minOffers, boolean sort) {

		String query =  sanitize(initialQuery);


		// Logging
		globalstatsLogger.info("global search : {}",initialQuery);

		Criteria c = null;
		if (StringUtils.isNumeric(query)) {
			// Showing even if no offers when by GTIN
			c = new Criteria("attributes.referentielAttributes.GTIN.keyword").is(initialQuery);
		}
		else {
			// TODO(security) : sanitize, web imput !!
			c = 	new Criteria("names.offerNames").matchesAll(Arrays.asList(query.split(" ")))
					.and("offersCount").greaterThanEqual(1)
					.and(aggregatedDataRepository.getValidDateQuery())
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
	@Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public VerticalSearchResponse verticalSearch(VerticalConfig vertical, VerticalSearchRequest request) {

		VerticalSearchResponse vsr = new VerticalSearchResponse();

		List<AttributeConfig> customAttrFilters = vertical.verticalFilters().stream().filter(Objects::nonNull).toList();

		Criteria criterias = new Criteria("vertical").is(vertical.getId())
				.and(aggregatedDataRepository.getValidDateQuery())
				.and(new Criteria("excluded"). is(request.isExcluded()))
				;

//		// min price
		if (null != request.getMinPrice()) {
			criterias.and(new Criteria("price.minPrice.price").greaterThanEqual(Math.floor(request.getMinPrice())));
		}

		// max price
		if (null != request.getMaxPrice()) {
			criterias.and(new Criteria("price.minPrice.price").lessThanEqual(Math.ceil(request.getMaxPrice())));
		}


		// Adding custom numeric filters		
		for (NumericRangeFilter filter : request.getNumericFilters()) {
			criterias.and(new Criteria(filter.getKey()).lessThanEqual(filter.getMaxValue()) );
			criterias.and(new Criteria(filter.getKey()).greaterThanEqual(filter.getMinValue()));
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
//

		// Setting the query
		NativeQueryBuilder esQuery = new NativeQueryBuilder().withQuery(new CriteriaQuery(criterias));

		// Pagination
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
				//
				.withAggregation("conditions", 	Aggregation.of(a -> a.terms(ta -> ta.field("price.conditions").missing(OTHER_BUCKET).size(3)  ))	)
				//
				//				// TODO : size from conf
				.withAggregation("brands", 	Aggregation.of(a -> a.terms(ta -> ta.field("attributes.referentielAttributes.BRAND.keyword").missing(OTHER_BUCKET).size(100)  ))	)
				//
				//				// TODO : size from conf
				.withAggregation("country", 	Aggregation.of(a -> a.terms(ta -> ta.field("gtinInfos.country").missing(OTHER_BUCKET).size(100)  ))	)
				//
				;
		////
		// Sort order
		/////

		if (null == request.getSortField()) {
			esQuery = esQuery.withSort(Sort.by("offersCount").descending());
		} else {
			// TODO : check value, remove ignorecase
			if (request.getSortOrder().equalsIgnoreCase("DESC") ) {
				esQuery = esQuery.withSort(Sort.by(request.getSortField()).descending() );
			} else if (request.getSortOrder().equalsIgnoreCase("ASC") ){
				esQuery = esQuery.withSort(Sort.by(request.getSortField()).ascending() );
			} else {
				throw new RuntimeException("implement");
			}
		}

		// Adding custom attributes terms filters aggregations
		for (AttributeConfig attrConfig : customAttrFilters) {
			esQuery = esQuery
					// TODO : size from conf
					.withAggregation(attrConfig.getKey(), 	Aggregation.of(a -> a.terms(ta -> ta.field("attributes.aggregatedAttributes."+attrConfig.getKey()+".value.keyword").missing(OTHER_BUCKET).size(100)  ))	);
		}
		
		
		
		// Adding custom range aggregations
		for (NumericRangeFilter filter: request.getNumericFilters()) {
			esQuery = esQuery
					// TODO : size from conf
			.withAggregation("min"+filter.getKey(),  	Aggregation.of(a -> a.min(ta -> ta.field(filter.getKey()))))
			.withAggregation("max"+filter.getKey(), 	Aggregation.of(a -> a.max(ta -> ta.field(filter.getKey()))))		;		
		
		}
				
		SearchHits<Product> results = aggregatedDataRepository.search(esQuery.build(),ProductRepository.MAIN_INDEX_NAME);


		// Handling aggregations results if relevant
		//TODO(gof) : this cast should be avoided
		ElasticsearchAggregations aggregations = (ElasticsearchAggregations)results.getAggregations();


		///////
		// Numeric aggregations
		///////
		MinAggregate minPrice = aggregations.get("min_price").aggregation().getAggregate().min();
		MaxAggregate maxPrice = aggregations.get("max_price").aggregation().getAggregate().max();
		MaxAggregate maxOffers = aggregations.get("max_offers").aggregation().getAggregate().max();
		MinAggregate minOffers = aggregations.get("min_offers").aggregation().getAggregate().min();
		//
		vsr.setMaxPrice(maxPrice.value());
		vsr.setMinPrice(minPrice.value());
		vsr.setMaxOffers(Double.valueOf(maxOffers.value()).intValue());
		vsr.setMinOffers(Double.valueOf(minOffers.value()).intValue());


		
		// Adding custom range aggregations
		for (NumericRangeFilter filter: request.getNumericFilters()) {
			
			MinAggregate min = aggregations.get("min"+filter.getKey()).aggregation().getAggregate().min();
			MaxAggregate max= aggregations.get("max"+filter.getKey()).aggregation().getAggregate().max();
		
			NumericRangeFilter nrf = new NumericRangeFilter();
			nrf.setMaxValue(min.value());
			nrf.setMinValue(max.value());
			
		}
		
		
		///////
		// Item condition
		///////

		StringTermsAggregate productSate  =  aggregations.get("conditions").aggregation().getAggregate().sterms();
		for (StringTermsBucket b :   productSate.buckets().array()) {
			vsr.getConditions().add (new VerticalFilterTerm(b.key().stringValue(), b.docCount()));
		}
		vsr.getConditions().sort((o1, o2) -> o2.getCount().compareTo(o1.getCount()));

		//TODO : Add other
		//		// Adding others
		//		if (null != productSate.getBucketByKey(OTHER_BUCKET)) {
		//			vsr.getConditions().add ( new VerticalFilterTerm(OTHER_BUCKET,productSate.getBucketByKey(OTHER_BUCKET).getDocCount()));
		//		}
		//

		///////
		// Brands
		///////

		StringTermsAggregate brands  =  aggregations.get("brands").aggregation().getAggregate().sterms() ;
		for (StringTermsBucket b :   brands.buckets().array()) {
			vsr.getBrands().add (new VerticalFilterTerm(b.key().stringValue(), b.docCount()));
		}
		vsr.getBrands().sort((o1, o2) -> o2.getCount().compareTo(o1.getCount()));
		//		TODO : Add other
		//		// Adding others
		//		if (null != brands.getBucketByKey(OTHER_BUCKET)) {
		//			vsr.getBrands().add ( new VerticalFilterTerm(OTHER_BUCKET,brands.getBucketByKey(OTHER_BUCKET).getDocCount()));
		//		}

		///////
		// Brands
		///////

		StringTermsAggregate countries  =  aggregations.get("country").aggregation().getAggregate().sterms() ;
		for (StringTermsBucket bucket : countries.buckets().array()) {
			vsr.getCountries().add (new VerticalFilterTerm(bucket.key().stringValue(), bucket.docCount()));
		}
		vsr.getCountries().sort((o1, o2) -> o2.getCount().compareTo(o1.getCount()));
		//TODO: add missing
		//		if (null != countries.buckets().keyed().get(OTHER_BUCKET)) {
		////			vsr.getCountries().add ( new VerticalFilterTerm(OTHER_BUCKET,countries. buckets().keyed().get(OTHER_BUCKET).docCount()));
		////		}


		//////////////
		/// Attr filters
		//////////////


		//		// Handling custom filters aggregations
		for (AttributeConfig attrConfig : customAttrFilters) {
			StringTermsAggregate agg  =  aggregations.get(attrConfig.getKey()).aggregation().getAggregate().sterms() ;
			vsr.getCustomFilters().put(attrConfig, new ArrayList<>());
			for (StringTermsBucket bucket : agg.buckets().array()) {

				if (!bucket.key().stringValue().equals(OTHER_BUCKET)) {
					vsr.getCustomFilters().get(attrConfig).add (new VerticalFilterTerm(bucket.key().stringValue(), bucket.docCount()));
				}
			}

			if (attrConfig.getAttributeValuesOrdering().equals(org.open4goods.config.yml.attributes.Order.COUNT ) ) {
				vsr.getCustomFilters().get(attrConfig).sort((o1, o2) -> o2.getCount().compareTo(o1.getCount()));
			}
			else {
				vsr.getCustomFilters().get(attrConfig).sort(Comparator.comparing(VerticalFilterTerm::getText));
			}

			// TODO : add missing
			//			// Adding others
			//			if (null != agg.getBucketByKey(OTHER_BUCKET)) {
			//				vsr.getCustomFilters().get(attrConfig).add ( new VerticalFilterTerm(OTHER_BUCKET,agg.getBucketByKey(OTHER_BUCKET).getDocCount()));
			//			}
		}

		//		// Setting the response
		vsr.setTotalResults(results.getTotalHits());
		vsr.setData(results.get().map(SearchHit::getContent).toList());

		vsr.setVerticalConfig(vertical);
		vsr.setRequest(request);
		String queryString = StringUtils.join(criterias.getCriteriaChain(), "\n-> ");
		verticalstatsLogger.info("Searching in vertical {} : {} results for query \n-> {}", vertical.getId(), vsr.getTotalResults(), queryString);
//		verticalstatsLogger.info("Searching in vertical {} : {}",vertical.getId(), request.toString());

		return vsr;
	}


	//	/**
	//	 *
	//	 * @param categorie
	//	 * @return all Product for a categorie
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
