package org.open4goods.nudgerfrontapi.service;

import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;

import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.Currency;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.nudgerfrontapi.config.properties.SearchProperties;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.dto.search.AggregationBucketDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto.Agg;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto.AggType;
import org.open4goods.nudgerfrontapi.dto.search.AggregationResponseDto;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.Filter;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterField;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterOperator;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterValueType;
import org.open4goods.model.Localisable;
import org.open4goods.nudgerfrontapi.dto.search.SemanticScoreDiagnosticsDto;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.embedding.service.DjlTextEmbeddingService;
import org.open4goods.embedding.util.EmbeddingVectorUtils;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.HistogramAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.HistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.MissingAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StatsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.json.JsonData;

/**
 * Dedicated search service tailored for the frontend API.
 *
 * <p>
 * The implementation mirrors the behaviour of the historical
 * {@code verticalSearch} method located in the commons module while remaining
 * self contained. It supports vertical scoping, textual queries and aggregation
 * descriptors defined by {@link AggregationRequestDto} so the Nuxt frontend can
 * build charts with a single API call.
 * </p>
 */
@Service
public class SearchService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchService.class);
	private static final String MISSING_BUCKET = "ES-UNKNOWN";
	private static final int DEFAULT_BUCKET_COUNT = 10;
	private static final int DEFAULT_TERMS_SIZE = 50;
	private static final int GLOBAL_SEARCH_LIMIT = 20;
	private static final int SUGGEST_RESULT_LIMIT = 5;
	private static final String[] SUGGEST_SOURCE_INCLUDES = { "attributes.referentielAttributes.MODEL",
			"attributes.referentielAttributes.BRAND", "attributes.referentielAttributes.GTIN", "coverImagePath",
			"vertical", "scores.ECOSCORE", "price.minPrice.price", "price.minPrice.currency" };
	private static final String DEFAULT_LANGUAGE_KEY = "default";
	private static final String OFFER_NAMES_DENSITY_SCRIPT = """
			if (params.tokens == null || params.tokens.isEmpty()) { return _score; }
			def offers = params['_source'].offerNames;
			if (offers == null) { return _score; }
			double matched = 0;
			for (token in params.tokens) {
			    if (token == null) { continue; }
			    for (offer in offers) {
			        if (offer == null) { continue; }
			        if (offer.toLowerCase().contains(token)) {
			            matched += 1;
			            break;
			        }
			    }
			}
			double density = matched / params.tokens.size();
			if (density <= 0) { return _score; }
			return _score * (1.0 + density);
			""";
	private static final String EXCLUDED_FIELD = "excluded";
	private static final String EXCLUDED_CAUSES_FIELD = FilterField.excludedCauses.fieldPath();
	private static final Set<String> BOOLEAN_AGGREGATION_FIELDS = Set.of();
	private static final double OFFERS_COUNT_SEMANTIC_BOOST = 0.05d;
	private static final String SORT_FIELD_PRICE = "price.minPrice.price";
	private static final String SORT_FIELD_BRAND = "attributes.referentielAttributes.BRAND";
	private static final String SORT_FIELD_MODEL = "attributes.referentielAttributes.MODEL";
	private static final String SORT_FIELD_ECOSCORE = "scores.ECOSCORE.value";

	private final ProductRepository repository;
	private final VerticalsConfigService verticalsConfigService;
	private final ProductMappingService productMappingService;
	private final ApiProperties apiProperties;
	private final SearchProperties searchProperties;
	private final DjlTextEmbeddingService textEmbeddingService;
	private final DjlEmbeddingProperties embeddingProperties;
	private volatile List<VerticalSuggestionEntry> verticalSuggestions = List.of();

	public SearchService(ProductRepository repository, VerticalsConfigService verticalsConfigService,
			@Lazy ProductMappingService productMappingService, ApiProperties apiProperties,
			SearchProperties searchProperties, DjlTextEmbeddingService textEmbeddingService,
			DjlEmbeddingProperties embeddingProperties) {
		this.repository = repository;
		this.verticalsConfigService = verticalsConfigService;
		this.productMappingService = productMappingService;
		this.apiProperties = apiProperties;
		this.searchProperties = searchProperties;
		this.textEmbeddingService = textEmbeddingService;
		this.embeddingProperties = embeddingProperties;
	}

	@PostConstruct
	void initializeSuggestIndex() {
		rebuildVerticalSuggestions();
	}

	/**
	 * Executes a product search and computes aggregation buckets tailored for the
	 * frontend.
	 *
	 * @param pageable              pagination information requested by the caller
	 * @param verticalId            optional vertical identifier used to scope the
	 *                              search
	 * @param query                 optional free text query applied on offer names
	 * @param aggregationQuery      optional aggregation definition mirroring the
	 *                              Nuxt contract
	 * @param filters               optional structured filters applied on the
	 *                              search query
	 * @param allowSemanticFallback whether semantic search should be attempted
	 *                              when a text query is provided
	 * @return a {@link SearchResult} bundling {@link SearchHits} and aggregation
	 *         metadata
	 */
	@Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
	public SearchResult search(Pageable pageable, String verticalId, String query,
			AggregationRequestDto aggregationQuery, FilterRequestDto filters, boolean allowSemanticFallback, String searchType) {
		String sanitizedQuery = sanitize(query);
		String normalizedVerticalId = normalizeVerticalId(verticalId);

		FilterRequestDto normalizedFilters = normalizeFilters(filters);
		boolean hasExcludedOverride = hasExcludedOverride(normalizedFilters);
		boolean applyDefaultExclusion = !hasExcludedOverride;
		boolean textOnly = "TEXT".equalsIgnoreCase(searchType);
		boolean useSemanticSearch = !textOnly && allowSemanticFallback && StringUtils.hasText(sanitizedQuery);
		float[] semanticEmbedding = null;
		if (useSemanticSearch) {
			semanticEmbedding = buildNormalizedEmbedding(sanitizedQuery);
			if (semanticEmbedding == null) {
				useSemanticSearch = false;
			}
		}

		Query searchQuery = useSemanticSearch
				? buildSemanticFilterQuery(normalizedVerticalId, null, normalizedFilters, applyDefaultExclusion,
						VerticalScope.ANY)
				: buildProductSearchQuery(normalizedVerticalId, sanitizedQuery, normalizedFilters, applyDefaultExclusion);

		List<Agg> excludedAggregations = extractExcludedAggregations(aggregationQuery);
		boolean requiresAdminExcludedAggregation = applyDefaultExclusion && !excludedAggregations.isEmpty();
		Query adminAggregationQuery = requiresAdminExcludedAggregation
				? (useSemanticSearch
						? buildSemanticFilterQuery(normalizedVerticalId, null, normalizedFilters, false, VerticalScope.ANY)
						: buildProductSearchQuery(normalizedVerticalId, sanitizedQuery, normalizedFilters, false))
				: null;

		List<AggregationDescriptor> descriptors = new ArrayList<>();
		var nativeQueryBuilder = new org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder()
				.withQuery(searchQuery)
				.withSourceFilter(new org.springframework.data.elasticsearch.core.query.FetchSourceFilter(true, null,
						new String[] { "embedding" }));

		if (useSemanticSearch) {
			applySemanticSearch(nativeQueryBuilder, semanticEmbedding, searchQuery, pageable);
		} else {
			nativeQueryBuilder.withPageable(pageable);
		}

		if (aggregationQuery != null && aggregationQuery.aggs() != null) {
			for (Agg agg : aggregationQuery.aggs()) {
				if (!isValidAggregation(agg)) {
					continue;
				}
				switch (agg.type()) {
				case terms -> descriptors.add(configureTermsAggregation(nativeQueryBuilder, agg));
				case range -> descriptors.add(configureRangeAggregation(nativeQueryBuilder, agg));
				default -> LOGGER.warn("Unsupported aggregation type {}", agg.type());
				}
			}
		}

		SearchHits<Product> hits;
		try {
			hits = repository.search(nativeQueryBuilder.build(), ProductRepository.MAIN_INDEX_NAME);
		} catch (Exception e) {
			elasticLog(e);
			throw e;
		}
		if (useSemanticSearch) {
			hits = sliceSearchHits(hits, pageable);
		}
		List<AggregationResponseDto> aggregations = extractAggregationResults(hits, descriptors);
		if (requiresAdminExcludedAggregation) {
			List<AggregationResponseDto> overrides = computeExcludedAggregations(adminAggregationQuery,
					excludedAggregations);
			aggregations = mergeAggregationOverrides(aggregations, overrides);
		}
		return new SearchResult(hits, List.copyOf(aggregations));
	}

	/**
	 * Configure semantic KNN search clauses for a {@link NativeQueryBuilder}.
	 *
	 * @param builder    base query builder to enrich
	 * @param embedding normalized embedding vector
	 * @param filter     filter query scoping the semantic search
	 * @param pageable   requested page used to compute the KNN candidate size
	 */
	private void applySemanticSearch(NativeQueryBuilder builder, float[] embedding, Query filter, Pageable pageable) {
		int knnLimit = Math.max(pageable.getPageSize() * (pageable.getPageNumber() + 1), pageable.getPageSize());
		List<Float> queryVector = new ArrayList<>(embedding.length);
		for (float value : embedding) {
			queryVector.add(value);
		}

		co.elastic.clients.elasticsearch._types.KnnSearch knnSearch = co.elastic.clients.elasticsearch._types.KnnSearch
				.of(knn -> knn.field("embedding").queryVector(queryVector).k(knnLimit)
						.numCandidates(Math.max(knnLimit * 2, 50))
						.filter(filter == null ? List.of() : List.of(filter)));

		builder.withKnnSearches(knnSearch).withPageable(PageRequest.of(0, knnLimit)).withMinScore(searchProperties.getSemanticMinScore());
	}

	Query buildProductSearchQuery(String normalizedVerticalId, String sanitizedQuery, FilterRequestDto filters,
			boolean applyDefaultExclusion) {
		if (!StringUtils.hasText(sanitizedQuery)) {
			return buildBaseBoolQuery(normalizedVerticalId, sanitizedQuery, filters, applyDefaultExclusion, false, false);
		}
		return buildLexicalQuery(normalizedVerticalId, sanitizedQuery, filters, applyDefaultExclusion, false, false);
	}

	private Query buildBaseBoolQuery(String verticalId, String query, FilterRequestDto filters,
			boolean applyDefaultExclusion, boolean mustHaveVertical, boolean mustNotHaveVertical) {
		Long expiration = repository.expirationClause();

		return Query.of(q -> q.bool(b -> {
			b.filter(f -> f.range(r -> r.date(d -> d.field("lastChange").gt(expiration.toString()))));
			b.filter(f -> f.range(r -> r.number(n -> n.field("offersCount").gt(0.0))));
			if (applyDefaultExclusion) {
				b.filter(f -> f.term(t -> t.field(EXCLUDED_FIELD).value(false)));
			}
			if (StringUtils.hasText(verticalId)) {
				b.filter(f -> f.term(t -> t.field("vertical").value(verticalId)));
			} else if (mustHaveVertical) {
				b.filter(f -> f.exists(e -> e.field("vertical")));
			} else if (mustNotHaveVertical) {
				// TODO : Check
				//b.mustNot(m -> m.exists(e -> e.field("vertical")));
			}

			if (StringUtils.hasText(query)) {
				b.must(m -> m.multiMatch(mm -> mm
						.query(query)
						.fields("offerNames", "attributes.referentielAttributes.BRAND.keyword", "attributes.referentielAttributes.MODEL.keyword")
						.operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
						.type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)));
			}

			if (filters != null) {
				applyFilterRequest(filters, b);
			}
			return b;
		}));
	}

	private Query buildLexicalQuery(String verticalId, String query, FilterRequestDto filters,
			boolean applyDefaultExclusion, boolean mustHaveVertical, boolean mustNotHaveVertical) {

		return buildBaseBoolQuery(verticalId, query, filters, applyDefaultExclusion, mustHaveVertical,
				mustNotHaveVertical);
	}

	/**
	 * Builds a lexical query targeting products without a vertical assignment.
	 *
	 * @param sanitizedQuery        sanitized query string
	 * @param filters               optional filters to apply
	 * @param applyDefaultExclusion whether to apply the default exclusion filter
	 * @return a query limited to missing-vertical products
	 */
	private Query buildMissingVerticalSearchQuery(String sanitizedQuery, FilterRequestDto filters,
			boolean applyDefaultExclusion) {
		if (!StringUtils.hasText(sanitizedQuery)) {
			return buildBaseBoolQuery(null, sanitizedQuery, filters, applyDefaultExclusion, false, true);
		}
		return buildLexicalQuery(null, sanitizedQuery, filters, applyDefaultExclusion, false, true);
	}

	private String normalizeVerticalId(String verticalId) {
		if (!StringUtils.hasText(verticalId)) {
			return null;
		}
		if (verticalsConfigService.getConfigById(verticalId) != null) {
			return verticalId;
		}
		LOGGER.warn("Requested vertical {} not found. Returning empty result set.", verticalId);
		return "__unknown__";
	}

	/**
	 * Execute a semantic-first global search strategy with a lexical fallback for
	 * missing-vertical results.
	 *
	 * @param query          raw user query
	 * @param domainLanguage localisation hint (currently unused but kept for future
	 *                       enhancements)
	 * @param filters        optional filter criteria scoped to the global search
	 * @param sort           optional sort definition for global search results
	 * @return grouped search results and unassigned hits when necessary
	 */
	public GlobalSearchResult globalSearch(String query, DomainLanguage domainLanguage, FilterRequestDto filters,
			Sort sort, String searchType) {

		String sanitizedQuery = sanitize(query);

		FilterRequestDto normalizedFilters = normalizeFilters(filters);

		CategorySuggestion verticalCta = findExactVerticalMatch(sanitizedQuery, domainLanguage);

		// TODO : I have no result here !
		List<GlobalSearchHit> missingVerticalResults = executeMissingVerticalLexicalSearch(sanitizedQuery, domainLanguage,
				normalizedFilters, sort);


		List<GlobalSearchHit> lexicalVerticalHits = executeVerticalLexicalSearch(sanitizedQuery, domainLanguage, normalizedFilters, sort);
		List<GlobalSearchVerticalGroup> verticalGroups = groupHitsByVertical(lexicalVerticalHits, sort);


		return new GlobalSearchResult(verticalGroups,
				missingVerticalResults,
				verticalCta,
				null);
	}

	/**
	 * Generate suggest data by combining vertical matches and product hits.
	 *
	 * @param query          user input fragment
	 * @param domainLanguage localisation hint for categories
	 * @return bundle of category and product suggestions
	 */
	@Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
	public SuggestResult suggest(String query, DomainLanguage domainLanguage) {
		String sanitizedQuery = sanitize(query);
		if (!StringUtils.hasText(sanitizedQuery)) {
			return new SuggestResult(List.of(), List.of());
		}

		List<String> categoryTokens = normalizedTokens(normalizeForComparison(sanitizedQuery));
		List<CategorySuggestion> categoryMatches = categoryTokens.isEmpty() ? List.of()
				: findCategoryMatches(categoryTokens, domainLanguage);

		List<String> scriptTokens = tokenizeForScript(sanitizedQuery);
		SearchHits<Product> productHits = executeSuggestProductSearch(sanitizedQuery, scriptTokens);
		List<ProductSuggestHit> productMatches = mapSuggestHits(productHits, domainLanguage);
		if (productMatches.isEmpty() && isSemanticSuggestFallbackEnabled()) {
			SearchHits<Product> semanticHits = executeSemanticSuggestProductSearch(sanitizedQuery);
			productMatches = mapSuggestHits(semanticHits, domainLanguage);
		}

		LOGGER.info("Suggest for {} : {} categories match, {} product matched", query, categoryMatches.size(),
				productMatches.size());
		return new SuggestResult(categoryMatches, productMatches);
	}

	/**
	 * Determines whether semantic fallback is enabled for suggest results.
	 *
	 * @return {@code true} when semantic fallback is enabled
	 */
	private boolean isSemanticSuggestFallbackEnabled() {
		return searchProperties.getSuggest().isSemanticFallbackEnabled();
	}

	private List<CategorySuggestion> findCategoryMatches(List<String> tokens, DomainLanguage domainLanguage) {
		if (tokens == null || tokens.isEmpty()) {
			return List.of();
		}
		String requestedLanguage = domainLanguage != null ? domainLanguage.languageTag() : null;
		List<VerticalSuggestionEntry> suggestions = verticalSuggestions;
		List<CategorySuggestion> matches = suggestions.stream()
				.filter(entry -> entry.matchesLanguage(requestedLanguage)).filter(entry -> entry.matches(tokens))
				.limit(SUGGEST_RESULT_LIMIT).map(this::toCategorySuggestion).toList();

		if (matches.isEmpty() && StringUtils.hasText(requestedLanguage)) {
			matches = suggestions.stream().filter(entry -> entry.matches(tokens)).limit(SUGGEST_RESULT_LIMIT)
					.map(this::toCategorySuggestion).toList();
		}
		return matches;
	}

	private CategorySuggestion toCategorySuggestion(VerticalSuggestionEntry entry) {
		return new CategorySuggestion(entry.verticalId(), entry.imageSmall(), entry.title(), entry.url());
	}

	private List<ProductSuggestHit> mapSuggestHits(SearchHits<Product> hits, DomainLanguage domainLanguage) {
		if (hits == null || hits.isEmpty()) {
			return List.of();
		}
		return hits.getSearchHits().stream().map(hit -> mapSuggestHit(hit, domainLanguage)).filter(Objects::nonNull)
				.toList();
	}

	private ProductSuggestHit mapSuggestHit(SearchHit<Product> hit, DomainLanguage domainLanguage) {
		if (hit == null || hit.getContent() == null) {
			return null;
		}
		Product product = hit.getContent();
		Map<ReferentielKey, String> referentiel = product.getAttributes() != null
				? product.getAttributes().getReferentielAttributes()
				: null;
		String model = referentiel != null ? referentiel.get(ReferentielKey.MODEL) : null;
		String brand = referentiel != null ? referentiel.get(ReferentielKey.BRAND) : null;
		String gtin = referentiel != null ? referentiel.get(ReferentielKey.GTIN) : null;
		Score ecoscore = product.ecoscore();
		Double ecoscoreValue = ecoscore != null ? ecoscore.getValue() : null;
		AggregatedPrice bestPrice = product.bestPrice();
		Double bestPriceValue = bestPrice != null ? bestPrice.getPrice() : null;
		Currency bestPriceCurrency = bestPrice != null ? bestPrice.getCurrency() : null;
		String prettyName = null;
		if (product.getNames() != null && product.getNames().getPrettyName() != null) {
			prettyName = localise(product.getNames().getPrettyName(), domainLanguage);
		}
		return new ProductSuggestHit(model, brand, gtin, product.getCoverImagePath(), product.getVertical(),
				ecoscoreValue, bestPriceValue, bestPriceCurrency, (double) hit.getScore(), prettyName);
	}

	private SearchHits<Product> executeSuggestProductSearch(String sanitizedQuery, List<String> tokens) {
		if (!StringUtils.hasText(sanitizedQuery)) {
			return null;
		}
		Query query = buildSuggestProductQuery(sanitizedQuery, tokens);
		NativeQueryBuilder builder = new NativeQueryBuilder().withQuery(query)
				.withPageable(PageRequest.of(0, SUGGEST_RESULT_LIMIT))
				.withSourceFilter(new FetchSourceFilter(true, SUGGEST_SOURCE_INCLUDES, null));
		try {
			return repository.search(builder.build(), ProductRepository.MAIN_INDEX_NAME);
		} catch (Exception e) {
			elasticLog(e);
			throw e;
		}
	}

	/**
	 * Executes a semantic search for suggest when text prefix queries yield no products.
	 *
	 * @param sanitizedQuery normalized text query
	 * @return semantic suggest hits, or {@code null} if embeddings are unavailable
	 */
	private SearchHits<Product> executeSemanticSuggestProductSearch(String sanitizedQuery) {
		float[] embedding = buildNormalizedEmbedding(sanitizedQuery);
		if (embedding == null) {
			return null;
		}

		Query filterQuery = buildSuggestFilterQuery();

		int knnLimit = SUGGEST_RESULT_LIMIT;
		List<Float> queryVector = new ArrayList<>(embedding.length);
		for (float value : embedding) {
			queryVector.add(value);
		}

		co.elastic.clients.elasticsearch._types.KnnSearch knnSearch = co.elastic.clients.elasticsearch._types.KnnSearch
				.of(knn -> knn.field("embedding").queryVector(queryVector).k(knnLimit)
						.numCandidates(Math.max(knnLimit * 2, 50))
						.filter(filterQuery == null ? List.of() : List.of(filterQuery)));

		NativeQueryBuilder builder = new NativeQueryBuilder().withQuery(filterQuery).withKnnSearches(knnSearch)
				.withPageable(PageRequest.of(0, knnLimit))
				.withSourceFilter(new FetchSourceFilter(true, SUGGEST_SOURCE_INCLUDES, null))
				.withMinScore(searchProperties.getSemanticMinScore());

		try {
			return repository.search(builder.build(), ProductRepository.MAIN_INDEX_NAME);
		} catch (Exception e) {
			elasticLog(e);
			throw e;
		}
	}

	private Query buildSuggestProductQuery(String sanitizedQuery, List<String> tokens) {
		Long expiration = repository.expirationClause();

		return Query.of(q -> q.functionScore(fs -> {
			fs.scoreMode(FunctionScoreMode.Multiply);
			fs.boostMode(FunctionBoostMode.Multiply);
			fs.query(inner -> inner.bool(b -> {
				b.filter(f -> f.range(r -> r.date(d -> d.field("lastChange").gt(expiration.toString()))));
				b.filter(f -> f.range(r -> r.number(n -> n.field("offersCount").gt(0.0))));
				b.filter(f -> f.term(t -> t.field(EXCLUDED_FIELD).value(false)));
				b.filter(f -> f.exists(e -> e.field("vertical")));
				b.must(m -> m.matchPhrasePrefix(mq -> mq.field("offerNames").query(sanitizedQuery)));
				return b;
			}));
			if (tokens != null && !tokens.isEmpty()) {
				fs.functions(func -> func.scriptScore(ss -> ss.script(Script.of(s -> s.lang("painless")
						.source(OFFER_NAMES_DENSITY_SCRIPT).params(Map.of("tokens", JsonData.of(tokens)))))));
			}
			return fs;
		}));
	}

	/**
	 * Builds the filter query shared by suggest searches, excluding text criteria.
	 *
	 * @return query containing suggest filters
	 */
	private Query buildSuggestFilterQuery() {
		Long expiration = repository.expirationClause();

		return Query.of(q -> q.bool(b -> {
			b.filter(f -> f.range(r -> r.date(d -> d.field("lastChange").gt(expiration.toString()))));
			b.filter(f -> f.range(r -> r.number(n -> n.field("offersCount").gt(0.0))));
			b.filter(f -> f.term(t -> t.field(EXCLUDED_FIELD).value(false)));
			return b;
		}));
	}

	private void rebuildVerticalSuggestions() {
		List<VerticalSuggestionEntry> entries = verticalsConfigService.getConfigsWithoutDefault().stream()
				.flatMap(config -> buildSuggestionsForConfig(config).stream()).toList();
		verticalSuggestions = List.copyOf(entries);
	}

	private List<VerticalSuggestionEntry> buildSuggestionsForConfig(VerticalConfig config) {
		if (config == null) {
			return List.of();
		}
		List<VerticalSuggestionEntry> entries = new ArrayList<>();
		if (config.getI18n() != null) {
			config.getI18n().forEach((languageKey, elements) -> buildVerticalSuggestion(config, languageKey, elements)
					.ifPresent(entries::add));
		}
		if (entries.isEmpty()) {
			buildMinimalVerticalSuggestion(config).ifPresent(entries::add);
		}
		return entries;
	}

	private Optional<VerticalSuggestionEntry> buildVerticalSuggestion(VerticalConfig config, String languageKey,
			ProductI18nElements i18n) {
		if (config == null || i18n == null) {
			return Optional.empty();
		}
		String verticalId = config.getId();
		String title = StringUtils.hasText(i18n.getVerticalHomeTitle()) ? i18n.getVerticalHomeTitle().trim() : null;
		String url = StringUtils.hasText(i18n.getVerticalHomeUrl()) ? i18n.getVerticalHomeUrl().trim() : null;
		if (!StringUtils.hasText(verticalId) || !StringUtils.hasText(title) || !StringUtils.hasText(url)) {
			return Optional.empty();
		}
		String language = StringUtils.hasText(languageKey) ? languageKey.trim() : DEFAULT_LANGUAGE_KEY;
		String normalizedTitle = normalizeForComparison(title);
		String normalizedId = normalizeForComparison(verticalId);
		String normalizedUrl = normalizeForComparison(url);
		String imageSmall = buildVerticalImageSmall(verticalId);
		VerticalSuggestionEntry entry = new VerticalSuggestionEntry(verticalId, language, imageSmall, title, url,
				normalizedTitle, normalizedId, normalizedUrl);
		return Optional.of(entry);
	}

	private Optional<VerticalSuggestionEntry> buildMinimalVerticalSuggestion(VerticalConfig config) {
		if (config == null || !StringUtils.hasText(config.getId())) {
			return Optional.empty();
		}
		String verticalId = config.getId().trim();
		String normalizedId = normalizeForComparison(verticalId);
		String imageSmall = buildVerticalImageSmall(verticalId);
		VerticalSuggestionEntry entry = new VerticalSuggestionEntry(verticalId, DEFAULT_LANGUAGE_KEY, imageSmall,
				verticalId, verticalId, normalizedId, normalizedId, normalizedId);
		return Optional.of(entry);
	}

	private String buildVerticalImageSmall(String verticalId) {
		if (!StringUtils.hasText(apiProperties.getResourceRootPath()) || !StringUtils.hasText(verticalId)) {
			return null;
		}
		return apiProperties.getResourceRootPath() + "/images/verticals/" + verticalId + "-100.webp";
	}

	private String normalizeForComparison(String value) {
		if (!StringUtils.hasText(value)) {
			return "";
		}
		String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
				.replaceAll("[^\\p{Alnum}\\s]", " ").replaceAll("\\s+", " ").trim();
		return normalized.toLowerCase(Locale.ROOT);
	}

	private List<String> normalizedTokens(String normalized) {
		if (!StringUtils.hasText(normalized)) {
			return List.of();
		}
		return Arrays.stream(normalized.split("\\s+")).map(String::trim).filter(StringUtils::hasText).distinct()
				.toList();
	}

	private List<String> tokenizeForScript(String value) {
		if (!StringUtils.hasText(value)) {
			return List.of();
		}
		return Arrays.stream(value.toLowerCase(Locale.ROOT).split("\\s+")).map(String::trim)
				.filter(StringUtils::hasText).distinct().toList();
	}

	private boolean isValidAggregation(Agg agg) {
		return agg != null && StringUtils.hasText(agg.name()) && StringUtils.hasText(agg.field()) && agg.type() != null;
	}

	FilterRequestDto normalizeFilters(FilterRequestDto filters) {
		if (filters == null) {
			return null;
		}
		List<Filter> normalizedLegacy = mergeRangeFilters(filters.filters());
		List<FilterRequestDto.FilterGroup> normalizedGroups = normalizeGroups(filters.filterGroups());
		return new FilterRequestDto(normalizedLegacy, normalizedGroups);
	}

	private List<FilterRequestDto.FilterGroup> normalizeGroups(List<FilterRequestDto.FilterGroup> filterGroups) {
		if (filterGroups == null) {
			return List.of();
		}
		List<FilterRequestDto.FilterGroup> normalized = new ArrayList<>();
		for (FilterRequestDto.FilterGroup group : filterGroups) {
			if (group == null) {
				continue;
			}
			List<Filter> must = mergeRangeFilters(group.must());
			List<Filter> should = group.should() == null ? List.of() : List.copyOf(group.should());
			if (!must.isEmpty() || !should.isEmpty()) {
				normalized.add(new FilterRequestDto.FilterGroup(must, should));
			}
		}
		return normalized;
	}

	private List<Filter> mergeRangeFilters(List<Filter> filters) {
		if (filters == null || filters.isEmpty()) {
			return List.of();
		}
		Map<String, RangeBounds> rangeByField = new LinkedHashMap<>();
		List<Filter> merged = new ArrayList<>();
		for (Filter filter : filters) {
			if (filter == null) {
				continue;
			}
			if (filter.operator() == FilterOperator.range && StringUtils.hasText(filter.field())) {
				String field = filter.field().trim();
				RangeBounds bounds = rangeByField.computeIfAbsent(field, ignored -> new RangeBounds());
				bounds.accept(filter.min(), filter.max());
			} else {
				merged.add(filter);
			}
		}
		for (Map.Entry<String, RangeBounds> entry : rangeByField.entrySet()) {
			RangeBounds bounds = entry.getValue();
			if (bounds.isEmpty()) {
				continue;
			}
			Double min = bounds.computeMin();
			Double max = bounds.computeMax();
			if (min != null && max != null && min > max) {
				continue;
			}
			merged.add(new Filter(entry.getKey(), FilterOperator.range, null, min, max));
		}
		return merged;
	}

	private void applyFilterRequest(FilterRequestDto filters,
			co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder builder) {
		if (filters.filters() != null) {
			filters.filters().stream().map(this::toQuery).filter(Objects::nonNull).forEach(builder::filter);
		}
		if (filters.filterGroups() != null) {
			for (FilterRequestDto.FilterGroup group : filters.filterGroups()) {
				Query groupQuery = buildGroupQuery(group);
				if (groupQuery != null) {
					builder.filter(groupQuery);
				}
			}
		}
	}

	private Query buildGroupQuery(FilterRequestDto.FilterGroup group) {
		if (group == null) {
			return null;
		}
		boolean hasShould = group.should() != null && !group.should().isEmpty();
		boolean hasMust = group.must() != null && !group.must().isEmpty();
		if (!hasMust && !hasShould) {
			return null;
		}
		return Query.of(q -> q.bool(b -> {
			if (hasMust) {
				group.must().stream().map(this::toQuery).filter(Objects::nonNull).forEach(b::filter);
			}
			if (hasShould) {
				group.should().stream().map(this::toQuery).filter(Objects::nonNull).forEach(b::should);
				b.minimumShouldMatch("1");
			}
			return b;
		}));
	}

	private boolean hasExcludedOverride(FilterRequestDto filters) {
		if (filters == null) {
			return false;
		}
		Stream<String> legacyFields = filters.filters() == null ? Stream.empty()
				: filters.filters().stream().filter(Objects::nonNull).map(Filter::field);

		Stream<String> groupFields = filters.filterGroups() == null ? Stream.empty()
				: filters.filterGroups().stream().filter(Objects::nonNull)
						.flatMap(group -> Stream.concat(group.must() == null ? Stream.empty() : group.must().stream(),
								group.should() == null ? Stream.empty() : group.should().stream()))
						.filter(Objects::nonNull).map(Filter::field);

		return Stream.concat(legacyFields, groupFields).filter(StringUtils::hasText).map(String::trim)
				.anyMatch(field -> field.equals(EXCLUDED_CAUSES_FIELD));
	}

	private Query toQuery(Filter filter) {
		if (filter == null || !StringUtils.hasText(filter.field()) || filter.operator() == null) {
			return null;
		}
		String fieldPath = filter.field().trim();
		FilterOperator operator = filter.operator();

		FilterValueType valueType = resolveValueType(fieldPath, operator);

		return switch (operator) {
		case term -> buildTermQuery(fieldPath, valueType, filter.terms());
		case range -> buildRangeQuery(fieldPath, filter.min(), filter.max());
		};
	}

	private Query buildTermQuery(String fieldPath, FilterValueType valueType, List<String> terms) {
		if (terms == null || terms.isEmpty()) {
			return null;
		}

		if (valueType == FilterValueType.numeric) {
			Collection<Double> numericTerms = parseNumericTerms(fieldPath, terms);
			if (numericTerms.isEmpty()) {
				return null;
			}
			List<FieldValue> values = numericTerms.stream().map(FieldValue::of).toList();
			return Query.of(q -> q.terms(t -> t.field(fieldPath).terms(v -> v.value(values))));
		}

		Set<String> sanitized = terms.stream().filter(StringUtils::hasText).map(String::trim)
				.collect(Collectors.toSet());
		if (sanitized.isEmpty()) {
			return null;
		}
		List<FieldValue> values = sanitized.stream().map(FieldValue::of).toList();
		return Query.of(q -> q.terms(t -> t.field(fieldPath).terms(v -> v.value(values))));
	}

	private Collection<Double> parseNumericTerms(String fieldPath, List<String> terms) {
		List<Double> values = new ArrayList<>();
		for (String term : terms) {
			if (!StringUtils.hasText(term)) {
				continue;
			}
			try {
				values.add(Double.valueOf(term.trim()));
			} catch (NumberFormatException ex) {
				LOGGER.warn("Ignoring filter term '{}' for field {} due to invalid number format", term, fieldPath, ex);
				return Collections.emptyList();
			}
		}
		return values;
	}

	private Query buildRangeQuery(String fieldPath, Double min, Double max) {
		if (min == null && max == null) {
			return null;
		}
		return Query.of(q -> q.range(r -> r.number(n -> {
			n.field(fieldPath);
			if (min != null) {
				n.gte(min);
			}
			if (max != null) {
				n.lte(max);
			}
			return n;
		})));
	}

	private FilterValueType resolveValueType(String fieldPath, FilterOperator operator) {
		return FilterField.fromFieldPath(fieldPath).map(FilterField::valueType)
				.orElseGet(() -> inferValueType(fieldPath, operator));
	}

	private FilterValueType inferValueType(String fieldPath, FilterOperator operator) {
		if (operator == FilterOperator.range) {
			return FilterValueType.numeric;
		}
		if (fieldPath != null && fieldPath.endsWith(".numericValue")) {
			return FilterValueType.numeric;
		}
		return FilterValueType.keyword;
	}

	private static final class RangeBounds {

		private Double highestMin;
		private Double lowestMax;
		private boolean hasMin;
		private boolean hasMax;

		void accept(Double min, Double max) {
			if (min != null) {
				hasMin = true;
				highestMin = highestMin == null ? min : Math.max(highestMin, min);
			}
			if (max != null) {
				hasMax = true;
				lowestMax = lowestMax == null ? max : Math.min(lowestMax, max);
			}
		}

		boolean isEmpty() {
			return !hasMin && !hasMax;
		}

		Double computeMin() {
			return highestMin;
		}

		Double computeMax() {
			return lowestMax;
		}
	}

	private boolean isBooleanAggregationField(String fieldPath) {
		if (!StringUtils.hasText(fieldPath)) {
			return false;
		}
		return BOOLEAN_AGGREGATION_FIELDS.contains(fieldPath);
	}

	private AggregationDescriptor configureTermsAggregation(
			org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder builder, Agg agg) {
		int size = agg.buckets() != null && agg.buckets() > 0 ? agg.buckets() : DEFAULT_TERMS_SIZE;
		boolean booleanField = isBooleanAggregationField(agg.field());
		String missingAggregationName = booleanField ? agg.name() + "_missing" : null;

		builder.withAggregation(agg.name(), Aggregation.of(a -> {
			a.terms(t -> {
				t.field(agg.field()).size(size);
				if (!booleanField) {
					t.missing(MISSING_BUCKET);
				}
				return t;
			});
			return a;
		}));

		if (booleanField) {
			builder.withAggregation(missingAggregationName, Aggregation.of(a -> a.missing(m -> m.field(agg.field()))));
		}

		return AggregationDescriptor.forTerms(agg, missingAggregationName);
	}

	private AggregationDescriptor configureRangeAggregation(
			org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder builder, Agg agg) {
		double interval = resolveRangeInterval(agg);

		String histogramName = agg.name();
		String missingName = agg.name() + "_missing";
		String statsName = agg.name() + "_stats";

		builder.withAggregation(histogramName, Aggregation.of(a -> a.histogram(h -> {
			h.field(agg.field()).interval(interval).minDocCount(1);
			if (agg.min() != null || agg.max() != null) {
				h.extendedBounds(b -> {
					if (agg.min() != null) {
						b.min(agg.min());
					}
					if (agg.max() != null) {
						b.max(agg.max());
					}
					return b;
				});
			}
			return h;
		}))).withAggregation(missingName, Aggregation.of(a -> a.missing(m -> m.field(agg.field()))))
				.withAggregation(statsName, Aggregation.of(a -> a.stats(s -> s.field(agg.field()))));

		return AggregationDescriptor.forRange(agg, interval, histogramName, missingName, statsName);
	}

	private double resolveRangeInterval(Agg agg) {
		if (agg.step() != null && agg.step() > 0d) {
			return agg.step();
		}
		int buckets = agg.buckets() != null && agg.buckets() > 0 ? agg.buckets() : DEFAULT_BUCKET_COUNT;
		return computeInterval(agg.min(), agg.max(), buckets);
	}

	private double computeInterval(Double min, Double max, int buckets) {
		double effectiveMin = min != null ? min : 0d;
		double effectiveMax = max != null ? max : effectiveMin + buckets;
		if (effectiveMax <= effectiveMin) {
			effectiveMax = effectiveMin + buckets;
		}
		double span = effectiveMax - effectiveMin;
		double interval = span / Math.max(1, buckets);
		if (interval <= 0d) {
			interval = Math.max(1d, Math.abs(effectiveMax));
		}
		return interval;
	}

	private List<AggregationResponseDto> extractAggregationResults(SearchHits<Product> hits,
			List<AggregationDescriptor> descriptors) {
		if (descriptors.isEmpty() || hits == null || hits.getAggregations() == null) {
			return Collections.emptyList();
		}

		if (!(hits.getAggregations() instanceof ElasticsearchAggregations aggregations)) {
			return Collections.emptyList();
		}

		List<AggregationResponseDto> responses = new ArrayList<>();
		for (AggregationDescriptor descriptor : descriptors) {
			if (descriptor.type == AggType.terms) {
				responses.add(extractTermsAggregation(aggregations, descriptor));
			} else if (descriptor.type == AggType.range) {
				responses.add(extractRangeAggregation(aggregations, descriptor));
			}
		}
		return responses;
	}

	/**
	 * Group mapped hits by vertical identifier.
	 *
	 * @param hits mapped global search hits
	 * @param sort optional sort definition for global search results
	 * @return grouped vertical results preserving the requested ordering
	 */
	private List<GlobalSearchVerticalGroup> groupHitsByVertical(List<GlobalSearchHit> hits, Sort sort) {
		if (hits == null || hits.isEmpty()) {
			return List.of();
		}

		Map<String, List<GlobalSearchHit>> grouped = new LinkedHashMap<>();
		for (GlobalSearchHit hit : hits) {
			if (hit == null || !StringUtils.hasText(hit.verticalId())) {
				continue;
			}
			grouped.computeIfAbsent(hit.verticalId(), key -> new ArrayList<>()).add(hit);
		}

		List<GlobalSearchVerticalGroup> groups = grouped.entrySet().stream()
				.map(entry -> new GlobalSearchVerticalGroup(entry.getKey(), List.copyOf(entry.getValue())))
				.toList();
		if (sort == null || sort.isUnsorted()) {
			return groups.stream()
					.map(group -> new GlobalSearchVerticalGroup(group.verticalId(),
							group.results().stream().sorted(Comparator.comparing(GlobalSearchHit::score).reversed())
									.toList()))
					.sorted(Comparator.comparing(
							(GlobalSearchVerticalGroup group) -> group.results().isEmpty() ? Double.NEGATIVE_INFINITY
									: group.results().get(0).score())
							.reversed())
					.toList();
		}
		return groups;
	}


	/**
	 * Executes semantic search for global search, returning grouped vertical hits
	 * plus missing-vertical results.
	 *
	 * <ol>
	 * <li>Run vector search on products with a vertical assigned</li>
	 * <li>Run a dedicated semantic search on products without a vertical</li>
	 * </ol>
	 *
	 * @param sanitizedQuery sanitized query string
	 * @param domainLanguage localisation hint
	 * @param filters        optional filters scoped to the global search
	 * @param sort           optional sort definition for global search results
	 * @return grouped semantic hits and missing-vertical results
	 */
	private SemanticGlobalSearchResult executeSemanticGlobalSearch(String sanitizedQuery, DomainLanguage domainLanguage,
			FilterRequestDto filters, Sort sort) {
		float[] embedding = buildNormalizedEmbedding(sanitizedQuery);
		if (embedding == null) {
			return new SemanticGlobalSearchResult(List.of(), List.of(), false, null);
		}

		Pageable pageable = PageRequest.of(0, GLOBAL_SEARCH_LIMIT);
		SearchHits<Product> hits = null;
		SearchHits<Product> missingHits = null;
		try {
			hits = executeSemanticSearch(null, null, embedding, filters, true, pageable, VerticalScope.REQUIRED);
			missingHits = executeSemanticSearch(null, sanitizedQuery, embedding, filters, true, pageable, VerticalScope.MISSING);
		} catch (Exception e) {
			LOGGER.error("Semantic search failed for query '{}'", sanitizedQuery, e);
			return new SemanticGlobalSearchResult(List.of(), List.of(), false, null);
		}

		List<GlobalSearchHit> verticalHits = mapHits(hits, domainLanguage, true, sort);
		List<GlobalSearchVerticalGroup> grouped = groupHitsByVertical(verticalHits, sort);
		List<GlobalSearchHit> missingVerticalResults = mapHits(missingHits, domainLanguage, true, sort);

		return new SemanticGlobalSearchResult(grouped, missingVerticalResults, true,
				buildSemanticDiagnostics(grouped, missingVerticalResults));
	}

	/**
	 * Executes a lexical fallback search scoped to products without a vertical
	 * assignment.
	 *
	 * @param sanitizedQuery sanitized query string
	 * @param domainLanguage localisation hint for result mapping
	 * @param filters        optional filters to apply
	 * @param sort           optional sort definition
	 * @return list of mapped hits without a vertical assignment
	 */
	private List<GlobalSearchHit> executeMissingVerticalLexicalSearch(String sanitizedQuery,
			DomainLanguage domainLanguage, FilterRequestDto filters, Sort sort) {


		Pageable pageable = PageRequest.of(0, GLOBAL_SEARCH_LIMIT);
		Query query = buildMissingVerticalSearchQuery(sanitizedQuery, filters, false);
		NativeQueryBuilder builder = new NativeQueryBuilder()
				.withQuery(query)
				.withPageable(pageable)
				.withSourceFilter(new FetchSourceFilter(true, null, new String[] { "embedding" }));


		SearchHits<Product> hits;
		try {
			hits = repository.search(builder.build(), ProductRepository.MAIN_INDEX_NAME);
		} catch (Exception e) {
			elasticLog(e);
			throw e;
		}

		return mapHits(hits, domainLanguage, false, sort);
	}

	/**
	 * Executes a lexical fallback search scoped to products WITH a vertical assignment.
	 *
	 * @param sanitizedQuery sanitized query string
	 * @param domainLanguage localisation hint for result mapping
	 * @param filters        optional filters to apply
	 * @param sort           optional sort definition
	 * @return list of mapped hits with a vertical assignment
	 */
	private List<GlobalSearchHit> executeVerticalLexicalSearch(String sanitizedQuery,
			DomainLanguage domainLanguage, FilterRequestDto filters, Sort sort) {


		Pageable pageable = PageRequest.of(0, GLOBAL_SEARCH_LIMIT);

		// Build a query similar to buildProductSearchQuery but ensuring vertical exists
		Query query = buildLexicalQuery(null, sanitizedQuery, filters, true, true, false);

		NativeQueryBuilder builder = new NativeQueryBuilder()
				.withQuery(query)
				.withPageable(pageable)
				.withSourceFilter(new FetchSourceFilter(true, null, new String[] { "embedding" }));

		SearchHits<Product> hits;
		try {
			hits = repository.search(builder.build(), ProductRepository.MAIN_INDEX_NAME);
		} catch (Exception e) {
			elasticLog(e);
			throw e;
		}

		return mapHits(hits, domainLanguage, false, sort);
	}



	/**
	 * Execute a semantic KNN search within a vertical using the same recency and
	 * offer guardrails as standard searches.
	 *
	 * @param verticalId     vertical identifier to scope results
	 * @param query          free-text query used to build the embedding
	 * @param domainLanguage localisation hint for result mapping
	 * @param pageNumber     zero-based page index
	 * @param pageSize       number of results per page
	 * @return list of mapped semantic hits
	 */
	public List<GlobalSearchHit> semanticSearch(String verticalId, String query, DomainLanguage domainLanguage,
			int pageNumber, int pageSize) {
		String sanitizedQuery = sanitize(query);
		String embeddingInput = buildQueryEmbeddingInput(sanitizedQuery);
		float[] embedding;
		try {
			embedding = textEmbeddingService.embed(embeddingInput);
		} catch (IllegalStateException ex) {
			LOGGER.warn("Semantic search unavailable because no embedding model is loaded: {}", ex.getMessage());
			return List.of();
		}

		if (embedding == null || embedding.length == 0) {
			LOGGER.info("Skipping semantic search because embedding is missing for query '{}'", sanitizedQuery);
			return List.of();
		}

		int knnLimit = Math.max(pageSize * (pageNumber + 1), pageSize);

		// To 512 dims
		embedding = IdHelper.to512(embedding);
		EmbeddingVectorUtils.normalizeL2(embedding);

		List<Float> queryVector = new ArrayList<>(embedding.length);
		for (float value : embedding) {
			queryVector.add(value);
		}

		Query filterQuery = buildSemanticFilterQuery(verticalId, null, null, true, VerticalScope.ANY);

		co.elastic.clients.elasticsearch._types.KnnSearch knnSearch = co.elastic.clients.elasticsearch._types.KnnSearch
				.of(knn -> knn.field("embedding").queryVector(queryVector).k(knnLimit)
						.numCandidates(Math.max(knnLimit * 2, 50))
						.filter(filterQuery == null ? List.of() : List.of(filterQuery)));

		org.springframework.data.elasticsearch.client.elc.NativeQuery knnQuery = new org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder()
				.withQuery(filterQuery).withKnnSearches(knnSearch)
				.withPageable(org.springframework.data.domain.PageRequest.of(0, knnLimit))
				.withSourceFilter(new FetchSourceFilter(true, null, new String[] { "embedding" })).build();

		SearchHits<Product> hits;
		try {
			hits = repository.search(knnQuery, ProductRepository.MAIN_INDEX_NAME);
		} catch (Exception e) {
			elasticLog(e);
			throw e;
		}

		List<SearchHit<Product>> sortedHits = sortSemanticHits(hits.getSearchHits(), Sort.unsorted());
		return sortedHits.stream().skip((long) pageNumber * pageSize).limit(pageSize)
				.map(hit -> mapHit(hit, domainLanguage, resolveSemanticBoostedScore(hit))).filter(Objects::nonNull)
				.toList();
	}

	private List<GlobalSearchHit> mapHits(SearchHits<Product> hits, DomainLanguage domainLanguage,
			boolean applyOffersCountBoost, Sort sort) {
		if (hits == null || hits.isEmpty()) {
			return List.of();
		}
		List<SearchHit<Product>> sortedHits = sortSemanticHits(hits.getSearchHits(), sort);
		return sortedHits.stream()
				.map(hit -> mapHit(hit, domainLanguage,
						applyOffersCountBoost ? resolveSemanticBoostedScore(hit) : hit.getScore()))
				.filter(Objects::nonNull)
				.toList();
	}

	private GlobalSearchHit mapHit(SearchHit<Product> hit, DomainLanguage domainLanguage) {
		return mapHit(hit, domainLanguage, hit.getScore());
	}

	private GlobalSearchHit mapHit(SearchHit<Product> hit, DomainLanguage domainLanguage, double score) {
		if (hit == null || hit.getContent() == null) {
			return null;
		}
		Product product = hit.getContent();
		Locale locale = resolveLocale(domainLanguage);
		ProductDto productDto = productMappingService.mapProduct(product, locale, null, domainLanguage, false);
		if (productDto == null) {
			return null;
		}
		return new GlobalSearchHit(product.getVertical(), productDto, score);
	}

	/**
	 * Executes a semantic search using the provided embedding vector.
	 *
	 * @param verticalId            optional vertical scope
	 * @param textQuery             optional text query for lexical matching (used for missing-vertical products)
	 * @param embedding             normalized embedding vector
	 * @param filters               filters to apply
	 * @param applyDefaultExclusion whether the default exclusion filter should be
	 *                              applied
	 * @param pageable              requested page information
	 * @param verticalScope         constraint on vertical availability
	 * @return search hits from the semantic query
	 */
	private SearchHits<Product> executeSemanticSearch(String verticalId, String textQuery, float[] embedding, FilterRequestDto filters,
			boolean applyDefaultExclusion, Pageable pageable, VerticalScope verticalScope) {
		Query filterQuery = buildSemanticFilterQuery(verticalId, textQuery, filters, applyDefaultExclusion, verticalScope);
		return executeSemanticSearchWithFilter(embedding, filterQuery, pageable);
	}

	/**
	 * Execute a semantic search using a pre-built filter query.
	 *
	 * @param embedding   normalized embedding vector
	 * @param filterQuery query used to scope semantic results
	 * @param pageable    requested page
	 * @return search hits from the semantic query
	 */
	private SearchHits<Product> executeSemanticSearchWithFilter(float[] embedding, Query filterQuery,
			Pageable pageable) {
		if (embedding == null || embedding.length == 0) {
			return null;
		}

		int knnLimit = Math.max(pageable.getPageSize() * (pageable.getPageNumber() + 1), pageable.getPageSize());
		List<Float> queryVector = new ArrayList<>(embedding.length);
		for (float value : embedding) {
			queryVector.add(value);
		}

		co.elastic.clients.elasticsearch._types.KnnSearch knnSearch = co.elastic.clients.elasticsearch._types.KnnSearch
				.of(knn -> knn.field("embedding").queryVector(queryVector).k(knnLimit)
						.numCandidates(Math.max(knnLimit * 2, 50))
						.filter(filterQuery == null ? List.of() : List.of(filterQuery)));

		NativeQueryBuilder builder = new NativeQueryBuilder().withQuery(filterQuery).withKnnSearches(knnSearch)
				.withPageable(PageRequest.of(0, knnLimit))
				.withSourceFilter(new FetchSourceFilter(true, null, new String[] { "embedding" }))
				.withMinScore(searchProperties.getSemanticMinScore());

		try {
			return repository.search(builder.build(), ProductRepository.MAIN_INDEX_NAME);
		} catch (Exception e) {
			elasticLog(e);
			throw e;
		}
	}

	/**
	 * Slice search hits according to the requested page.
	 *
	 * @param hits     raw semantic hits
	 * @param pageable requested page
	 * @return sliced hits respecting pagination offsets
	 */
	private SearchHits<Product> sliceSearchHits(SearchHits<Product> hits, Pageable pageable) {
		if (hits == null || hits.isEmpty()) {
			return hits;
		}

		List<SearchHit<Product>> sortedHits = sortSemanticHits(hits.getSearchHits(), pageable.getSort());

		int start = Math.toIntExact(pageable.getOffset());
		int end = Math.min(start + pageable.getPageSize(), sortedHits.size());
		if (start >= sortedHits.size()) {
			return new SearchHitsImpl<>(hits.getTotalHits(), hits.getTotalHitsRelation(), hits.getMaxScore(),
					Duration.ZERO, null, null, List.of(), hits.getAggregations(), hits.getSuggest(), null);
		}

		List<SearchHit<Product>> sliced = sortedHits.subList(start, end);
		return new SearchHitsImpl<>(hits.getTotalHits(), hits.getTotalHitsRelation(), hits.getMaxScore(), Duration.ZERO,
				null, null, sliced, hits.getAggregations(), hits.getSuggest(), null);
	}

	/**
	 * Sort semantic search hits according to the provided sort definition.
	 *
	 * @param hits search hits to sort
	 * @param sort optional sort definition
	 * @return sorted hits list
	 */
	private List<SearchHit<Product>> sortSemanticHits(List<SearchHit<Product>> hits, Sort sort) {
		if (hits == null || hits.isEmpty()) {
			return List.of();
		}
		Comparator<SearchHit<Product>> comparator = buildSemanticSortComparator(sort);
		return hits.stream().sorted(comparator).toList();
	}

	/**
	 * Build a comparator for semantic search hits based on user-defined sort rules.
	 *
	 * @param sort optional sort definition
	 * @return comparator used to order semantic hits
	 */
	private Comparator<SearchHit<Product>> buildSemanticSortComparator(Sort sort) {
		if (sort == null || sort.isUnsorted()) {
			return Comparator.comparingDouble(this::resolveSemanticBoostedScore).reversed();
		}

		Comparator<SearchHit<Product>> comparator = null;
		for (Sort.Order order : sort) {
			Comparator<Comparable<Object>> valueComparator = order.isDescending()
					? Comparator.nullsLast(Comparator.reverseOrder())
					: Comparator.nullsLast(Comparator.naturalOrder());
			Comparator<SearchHit<Product>> fieldComparator = Comparator.comparing(
					hit -> (Comparable<Object>) resolveSortValue(hit != null ? hit.getContent() : null,
							order.getProperty()),
					valueComparator);
			comparator = comparator == null ? fieldComparator : comparator.thenComparing(fieldComparator);
		}
		return comparator == null
				? Comparator.comparingDouble(this::resolveSemanticBoostedScore).reversed()
				: comparator.thenComparing(Comparator.comparingDouble(this::resolveSemanticBoostedScore).reversed());
	}

	/**
	 * Resolve the sort value for a given product and field mapping.
	 *
	 * @param product product used for sorting
	 * @param field   elastic field mapping requested by the client
	 * @return comparable sort value or {@code null} when unavailable
	 */
	private Comparable<?> resolveSortValue(Product product, String field) {
		if (product == null || !StringUtils.hasText(field)) {
			return null;
		}
		return switch (field) {
		case SORT_FIELD_PRICE -> {
			AggregatedPrice price = product.bestPrice();
			yield price != null ? price.getPrice() : null;
		}
		case "offersCount" -> product.getOffersCount();
		case SORT_FIELD_BRAND -> normalizeSortText(resolveReferentielValue(product, ReferentielKey.BRAND));
		case SORT_FIELD_MODEL -> normalizeSortText(resolveReferentielValue(product, ReferentielKey.MODEL));
		case SORT_FIELD_ECOSCORE -> {
			Score score = product.ecoscore();
			yield score != null ? score.getValue() : null;
		}
		case "creationDate" -> product.getCreationDate();
		case "lastChange" -> product.getLastChange();
		default -> null;
		};
	}

	private String resolveReferentielValue(Product product, ReferentielKey key) {
		if (product == null || product.getAttributes() == null
				|| product.getAttributes().getReferentielAttributes() == null) {
			return null;
		}
		return product.getAttributes().getReferentielAttributes().get(key);
	}

	private String normalizeSortText(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim().toLowerCase(Locale.ROOT);
	}

	/**
	 * Compute a lightly boosted semantic score based on the number of offers.
	 *
	 * @param hit semantic search hit to score
	 * @return boosted score value
	 */
	private double resolveSemanticBoostedScore(SearchHit<Product> hit) {
		if (hit == null) {
			return 0.0d;
		}

		double baseScore = hit.getScore();
		Product product = hit.getContent();
		if (product == null || product.getOffersCount() == null) {
			return baseScore;
		}

		int offersCount = Math.max(0, product.getOffersCount());
		if (offersCount == 0) {
			return baseScore;
		}

		double boost = Math.log10(offersCount + 1.0d) * OFFERS_COUNT_SEMANTIC_BOOST;
		return baseScore + boost;
	}



	/**
	 * Builds and normalizes the embedding vector for a query.
	 *
	 * @param sanitizedQuery sanitized query string
	 * @return normalized embedding vector, or {@code null} if unavailable
	 */
	private float[] buildNormalizedEmbedding(String sanitizedQuery) {
		if (!StringUtils.hasText(sanitizedQuery)) {
			return null;
		}
		String embeddingInput = buildQueryEmbeddingInput(sanitizedQuery);
		float[] embedding;
		try {
			embedding = textEmbeddingService.embed(embeddingInput);
		} catch (IllegalStateException ex) {
			LOGGER.warn("Semantic search unavailable because no embedding model is loaded: {}", ex.getMessage());
			return null;
		}

		if (embedding == null || embedding.length == 0) {
			LOGGER.info("Skipping semantic search because embedding is missing for query '{}'", sanitizedQuery);
			return null;
		}

		embedding = IdHelper.to512(embedding);
		EmbeddingVectorUtils.normalizeL2(embedding);
		return embedding;
	}

	private Locale resolveLocale(DomainLanguage domainLanguage) {
		if (domainLanguage == null || !StringUtils.hasText(domainLanguage.languageTag())) {
			return Locale.ROOT;
		}
		return Locale.forLanguageTag(domainLanguage.languageTag());
	}

	private AggregationResponseDto extractTermsAggregation(ElasticsearchAggregations aggregations,
			AggregationDescriptor descriptor) {
		var aggregation = aggregations.get(descriptor.histogramName);
		if (aggregation == null) {
			return new AggregationResponseDto(descriptor.request.name(), descriptor.request.field(), descriptor.type,
					List.of(), null, null);
		}

		var aggregate = aggregation.aggregation().getAggregate();
		List<AggregationBucketDto> buckets = new ArrayList<>();
		boolean booleanField = isBooleanAggregationField(descriptor.request.field());
		if (aggregate.isSterms()) {
			StringTermsAggregate terms = aggregate.sterms();
			for (StringTermsBucket bucket : terms.buckets().array()) {
				String key = normalizeTermsKey(bucket.key().stringValue(), booleanField);
				buckets.add(
						new AggregationBucketDto(key, null, bucket.docCount(), Objects.equals(key, MISSING_BUCKET)));
			}
		} else if (aggregate.isLterms()) {
			LongTermsAggregate terms = aggregate.lterms();
			for (LongTermsBucket bucket : terms.buckets().array()) {
				String key = normalizeTermsKey(bucket.key(), booleanField);
				buckets.add(
						new AggregationBucketDto(key, null, bucket.docCount(), Objects.equals(key, MISSING_BUCKET)));
			}
		}
		if (descriptor.missingName != null) {
			var missingAggregation = aggregations.get(descriptor.missingName);
			if (missingAggregation != null && missingAggregation.aggregation().getAggregate().isMissing()) {
				MissingAggregate missing = missingAggregation.aggregation().getAggregate().missing();
				if (missing.docCount() > 0) {
					buckets.add(new AggregationBucketDto(MISSING_BUCKET, null, missing.docCount(), true));
				}
			}
		}
		buckets.sort((a, b) -> Long.compare(b.count(), a.count()));
		return new AggregationResponseDto(descriptor.request.name(), descriptor.request.field(), descriptor.type,
				buckets, null, null);
	}

	private String normalizeTermsKey(String key, boolean booleanField) {
		if (!booleanField || !StringUtils.hasText(key)) {
			return key;
		}
		if ("1".equals(key) || Boolean.TRUE.toString().equalsIgnoreCase(key)) {
			return Boolean.TRUE.toString();
		}
		if ("0".equals(key) || Boolean.FALSE.toString().equalsIgnoreCase(key)) {
			return Boolean.FALSE.toString();
		}
		return key;
	}

	private String normalizeTermsKey(long key, boolean booleanField) {
		if (!booleanField) {
			return Long.toString(key);
		}
		if (key == 1L) {
			return Boolean.TRUE.toString();
		}
		if (key == 0L) {
			return Boolean.FALSE.toString();
		}
		return Long.toString(key);
	}

	private AggregationResponseDto extractRangeAggregation(ElasticsearchAggregations aggregations,
			AggregationDescriptor descriptor) {
		var histogramAgg = aggregations.get(descriptor.histogramName);
		var missingAgg = aggregations.get(descriptor.missingName);
		var statsAgg = aggregations.get(descriptor.statsName);

		List<AggregationBucketDto> buckets = new ArrayList<>();
		Double min = null;
		Double max = null;
		if (statsAgg != null && statsAgg.aggregation().getAggregate().isStats()) {
			StatsAggregate stats = statsAgg.aggregation().getAggregate().stats();
			min = stats.min();
			max = stats.max();
		}

		if (histogramAgg != null && histogramAgg.aggregation().getAggregate().isHistogram()) {
			HistogramAggregate histogram = histogramAgg.aggregation().getAggregate().histogram();
			for (HistogramBucket bucket : histogram.buckets().array()) {
				double from = bucket.key();
				double to = from + descriptor.interval;
				buckets.add(new AggregationBucketDto(Double.toString(from), to, bucket.docCount(), false));
			}
		}

		if (missingAgg != null && missingAgg.aggregation().getAggregate().isMissing()) {
			MissingAggregate missing = missingAgg.aggregation().getAggregate().missing();
			if (missing.docCount() > 0) {
				buckets.add(new AggregationBucketDto(MISSING_BUCKET, null, missing.docCount(), true));
			}
		}

		if (min == null && descriptor.request.min() != null) {
			min = descriptor.request.min();
		}
		if (max == null && descriptor.request.max() != null) {
			max = descriptor.request.max();
		}
		if ((min == null || max == null) && !buckets.isEmpty()) {
			AggregationBucketDto firstBucket = buckets.get(0);
			AggregationBucketDto lastBucket = buckets.get(buckets.size() - 1);
			if (min == null && firstBucket.key() != null) {
				min = Double.parseDouble(firstBucket.key());
			}
			if (max == null && lastBucket.to() != null) {
				max = lastBucket.to();
			}
		}

		return new AggregationResponseDto(descriptor.request.name(), descriptor.request.field(), descriptor.type,
				buckets, min, max);
	}

	private List<Agg> extractExcludedAggregations(AggregationRequestDto aggregationQuery) {
		if (aggregationQuery == null || aggregationQuery.aggs() == null) {
			return List.of();
		}
		return aggregationQuery.aggs().stream().filter(Objects::nonNull).filter(agg -> StringUtils.hasText(agg.field()))
				.filter(agg -> EXCLUDED_CAUSES_FIELD.equals(agg.field().trim())).toList();
	}

	private List<AggregationResponseDto> computeExcludedAggregations(Query query, List<Agg> aggregations) {
		if (query == null || aggregations == null || aggregations.isEmpty()) {
			return List.of();
		}

		var builder = new NativeQueryBuilder().withQuery(query).withPageable(Pageable.unpaged())
				.withSourceFilter(new FetchSourceFilter(true, null, new String[] { "embedding" }));

		List<AggregationDescriptor> descriptors = new ArrayList<>();
		for (Agg agg : aggregations) {
			descriptors.add(configureTermsAggregation(builder, agg));
		}

		SearchHits<Product> hits;
		try {
			hits = repository.search(builder.build(), ProductRepository.MAIN_INDEX_NAME);
		} catch (Exception e) {
			elasticLog(e);
			throw e;
		}
		return extractAggregationResults(hits, descriptors);
	}

	private List<AggregationResponseDto> mergeAggregationOverrides(List<AggregationResponseDto> original,
			List<AggregationResponseDto> overrides) {
		if (overrides == null || overrides.isEmpty()) {
			return original;
		}
		Map<String, AggregationResponseDto> overrideByName = overrides.stream().collect(Collectors
				.toMap(AggregationResponseDto::name, Function.identity(), (left, right) -> right, LinkedHashMap::new));

		List<AggregationResponseDto> merged = new ArrayList<>(original.size() + overrides.size());
		for (AggregationResponseDto response : original) {
			merged.add(overrideByName.getOrDefault(response.name(), response));
		}

		for (AggregationResponseDto response : overrides) {
			boolean alreadyPresent = original.stream().anyMatch(existing -> existing.name().equals(response.name()));
			if (!alreadyPresent) {
				merged.add(response);
			}
		}
		return List.copyOf(merged);
	}

	private String sanitize(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		String sanitized = value.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", " ");
		sanitized = sanitized.replaceAll("\\s+", " ").trim();
		return sanitized;
	}

	/**
	 * Builds the text input sent to the embedding model for search queries.
	 *
	 * @param sanitizedQuery sanitized query string or {@code null}
	 * @return prefixed query string, or {@code null} when no query is supplied
	 */
	private String buildQueryEmbeddingInput(String sanitizedQuery) {
		if (!StringUtils.hasText(sanitizedQuery)) {
			return null;
		}

		String prefix = embeddingProperties.getQueryPrefix();
		if (!StringUtils.hasText(prefix)) {
			return sanitizedQuery;
		}

		return prefix.trim() + " " + sanitizedQuery;
	}

	/**
	 * Builds the scoped filter query used for semantic search.
	 *
	 * @param normalizedVerticalId  normalized vertical identifier (or {@code null})
	 * @param textQuery             optional text query for lexical matching (used for missing-vertical products)
	 * @param filters               normalized filters applied by the caller
	 * @param applyDefaultExclusion whether to apply the default exclusion filter
	 * @param verticalScope         constraint on vertical presence
	 * @return a query combining semantic filters
	 */
	private Query buildSemanticFilterQuery(String normalizedVerticalId, String textQuery, FilterRequestDto filters,
			boolean applyDefaultExclusion, VerticalScope verticalScope) {
		Long expiration = repository.expirationClause();
		return Query.of(q -> q.bool(b -> {
			b.filter(f -> f.range(r -> r.date(d -> d.field("lastChange").gt(expiration.toString()))));
			b.filter(f -> f.range(r -> r.number(n -> n.field("offersCount").gt(0.0))));
			if (applyDefaultExclusion) {
				b.filter(f -> f.term(t -> t.field(EXCLUDED_FIELD).value(false)));
			}
			if (StringUtils.hasText(normalizedVerticalId)) {
				b.filter(f -> f.term(t -> t.field("vertical").value(normalizedVerticalId)));
			}
			if (verticalScope == VerticalScope.REQUIRED) {
				b.filter(f -> f.exists(e -> e.field("vertical")));
			} else if (verticalScope == VerticalScope.MISSING) {
				b.mustNot(m -> m.exists(e -> e.field("vertical")));
				// For missing-vertical products, require text match to ensure relevance
				if (StringUtils.hasText(textQuery)) {
					b.must(m -> m.matchPhrasePrefix(mq -> mq.field("offerNames").query(textQuery)));
				}
			}
			if (filters != null) {
				applyFilterRequest(filters, b);
			}
			return b;
		}));
	}

	private enum VerticalScope {
		ANY,
		REQUIRED,
		MISSING
	}

	/**
	 * Computes semantic score diagnostics when enabled for the global search.
	 *
	 * @param groups                grouped semantic results
	 * @param missingVerticalResult semantic results without a vertical assignment
	 * @return diagnostics object or {@code null} when disabled or empty
	 */
	private SemanticScoreDiagnosticsDto buildSemanticDiagnostics(
			List<GlobalSearchVerticalGroup> groups, List<GlobalSearchHit> missingVerticalResult) {
		if (!apiProperties.isSemanticDiagnosticsEnabled()) {
			return null;
		}
		List<Double> scores = new ArrayList<>();
		if (groups != null) {
			for (GlobalSearchVerticalGroup group : groups) {
				if (group == null || group.results() == null) {
					continue;
				}
				for (GlobalSearchHit hit : group.results()) {
					if (hit != null) {
						scores.add(hit.score());
					}
				}
			}
		}
		if (missingVerticalResult != null) {
			for (GlobalSearchHit hit : missingVerticalResult) {
				if (hit != null) {
					scores.add(hit.score());
				}
			}
		}
		if (scores.isEmpty()) {
			return null;
		}
		double max = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0d);
		double min = scores.stream().mapToDouble(Double::doubleValue).min().orElse(0d);
		double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0d);
		double variance = scores.stream().mapToDouble(score -> Math.pow(score - avg, 2)).average().orElse(0d);
		double stdDev = Math.sqrt(variance);
		return new SemanticScoreDiagnosticsDto(scores.size(), max, min, avg,
				stdDev);
	}

	/**
	 * Look for a vertical strictly matching the query to provide a navigational
	 * shortcut.
	 *
	 * @param query          user query
	 * @param domainLanguage localisation hint
	 * @return a category suggestion if a strict match is found
	 */
	private CategorySuggestion findExactVerticalMatch(String query, DomainLanguage domainLanguage) {
		if (!StringUtils.hasText(query)) {
			return null;
		}

		List<String> tokens = normalizedTokens(normalizeForComparison(query));
		if (tokens.isEmpty()) {
			return null;
		}

		// 1. Try to find a match in the suggestions
		List<CategorySuggestion> matches = findCategoryMatches(tokens, domainLanguage);

		if (matches.isEmpty()) {
			return null;
		}

		// 2. We want a "certain" match.
		// For now, we consider a certain match if we have exactly one proposal
		// and if the match quality is high (checked via token inclusion in
		// findCategoryMatches)
		if (matches.size() == 1) {
			return matches.get(0);
		}

		return null;
	}

	/**
	 * Global search result including grouped hits and missing-vertical results.
	 */
	public record GlobalSearchResult(List<GlobalSearchVerticalGroup> verticalGroups,
			List<GlobalSearchHit> missingVerticalResults,
			CategorySuggestion verticalCta,
			SemanticScoreDiagnosticsDto diagnostics) {

		public GlobalSearchResult {
			verticalGroups = List.copyOf(verticalGroups);
			missingVerticalResults = List.copyOf(missingVerticalResults);
		}
	}

	/**
	 * Semantic search output used by global search to separate vertical and
	 * missing-vertical hits.
	 */
	private record SemanticGlobalSearchResult(List<GlobalSearchVerticalGroup> verticalGroups,
			List<GlobalSearchHit> missingVerticalResults, boolean executed,
			SemanticScoreDiagnosticsDto diagnostics) {

		private SemanticGlobalSearchResult {
			verticalGroups = List.copyOf(verticalGroups);
			missingVerticalResults = List.copyOf(missingVerticalResults);
		}

		boolean hasResults() {
			return executed && (!verticalGroups.isEmpty() || !missingVerticalResults.isEmpty());
		}
	}

	/**
	 * Group of hits belonging to the same vertical.
	 */
	public record GlobalSearchVerticalGroup(String verticalId, List<GlobalSearchHit> results) {

		public GlobalSearchVerticalGroup {
			results = List.copyOf(results);
		}
	}

	/**
	 * Lightweight representation of a product hit used by the controller layer.
	 */
	public record GlobalSearchHit(String verticalId, ProductDto product, double score) {
	}

	/**
	 * Result returned by the suggest feature combining category and product
	 * matches.
	 */
	public record SuggestResult(List<CategorySuggestion> categoryMatches, List<ProductSuggestHit> productMatches) {

		public SuggestResult {
			categoryMatches = List.copyOf(categoryMatches);
			productMatches = List.copyOf(productMatches);
		}
	}

	/**
	 * Category suggestion projected from the in-memory vertical index.
	 */
	public record CategorySuggestion(String verticalId, String imageSmall, String verticalHomeTitle,
			String verticalHomeUrl) {
	}

	/**
	 * Product suggestion built from a lightweight Elasticsearch hit.
	 */
	public record ProductSuggestHit(String model, String brand, String gtin, String coverImagePath, String verticalId,
			Double ecoscoreValue, Double bestPrice, Currency bestPriceCurrency, Double score, String prettyName) {
	}

	private record VerticalSuggestionEntry(String verticalId, String languageKey, String imageSmall, String title,
			String url, String normalizedTitle, String normalizedId, String normalizedUrl) {

		boolean matchesLanguage(String requestedLanguage) {
			if (!StringUtils.hasText(requestedLanguage)) {
				return true;
			}
			return requestedLanguage.equalsIgnoreCase(languageKey);
		}

		boolean isDefaultLanguage() {
			return DEFAULT_LANGUAGE_KEY.equalsIgnoreCase(languageKey);
		}

		boolean matches(List<String> tokens) {
			if (tokens == null || tokens.isEmpty()) {
				return false;
			}
			return tokens.stream().anyMatch(token -> normalizedTitle.contains(token) || normalizedId.contains(token)
					|| normalizedUrl.contains(token));
		}
	}

	/**
	 * Wrapper holding the {@link SearchHits} and the aggregation response DTOs.
	 */
	public record SearchResult(SearchHits<Product> hits, List<AggregationResponseDto> aggregations) {
	}

	private record AggregationDescriptor(Agg request, AggType type, double interval, String histogramName,
			String missingName, String statsName) {

		private static AggregationDescriptor forTerms(Agg request, String missingName) {
			return new AggregationDescriptor(request, request.type(), 0d, request.name(), missingName, null);
		}

		private static AggregationDescriptor forRange(Agg request, double interval, String histogramName,
				String missingName, String statsName) {
			return new AggregationDescriptor(request, request.type(), interval, histogramName, missingName, statsName);
		}
	}

	private void elasticLog(Exception e) {
		if (e instanceof UncategorizedElasticsearchException) {
			Throwable cause = e.getCause();
			if (cause instanceof ElasticsearchException ee) {
				LOGGER.error("Elasticsearch error: " + ee.response());
			} else {
				LOGGER.error("Error : ", e);
			}
		} else {
			LOGGER.error("Error executing elasticsearch query", e);
		}
	}

	private String localise(Localisable<String, String> localisable, DomainLanguage domainLanguage) {
		if (localisable == null) {
			return null;
		}
		String language = domainLanguage != null ? domainLanguage.languageTag() : null;
		return localisable.i18n(language);
	}
}
