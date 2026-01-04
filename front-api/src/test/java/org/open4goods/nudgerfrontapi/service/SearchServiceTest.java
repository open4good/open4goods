package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationBucketDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto.Agg;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto.AggType;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.Filter;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterField;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterOperator;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.open4goods.embedding.service.DjlTextEmbeddingService;
import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;

/**
 * Unit tests for {@link SearchService} focusing on filter application.
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    private static final String EXCLUDED_FIELD = "excluded";

    @Mock
    private ProductRepository repository;

    @Mock
    private VerticalsConfigService verticalsConfigService;

    @Mock
    private ProductMappingService productMappingService;

    @Mock
    private ApiProperties apiProperties;

    @Mock
    private DjlTextEmbeddingService textEmbeddingService;

    private DjlEmbeddingProperties embeddingProperties;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        lenient().when(apiProperties.getResourceRootPath()).thenReturn("https://assets.nudger.test");
        lenient().when(repository.expirationClause()).thenReturn(0L);
        embeddingProperties = new DjlEmbeddingProperties();
        searchService = new SearchService(repository, verticalsConfigService, productMappingService, apiProperties, textEmbeddingService,
                embeddingProperties);
    }

    @Test
    void searchAppliesFiltersAndKeepsAggregations() {
        Pageable pageable = PageRequest.of(0, 20);
        when(verticalsConfigService.getConfigById("electronics")).thenReturn(new VerticalConfig());

        Product product = new Product(1L);
        SearchHit<Product> searchHit = new SearchHit<>(ProductRepository.MAIN_INDEX_NAME, "1", null, 1f, null,
                Map.of(), Map.of(), null, null, List.of(), product);
        Aggregate aggregate = Aggregate.of(a -> a.sterms(st -> st.buckets(b -> b.array(List.of(
                StringTermsBucket.of(bucket -> bucket.key(FieldValue.of(fv -> fv.stringValue("NEW"))).docCount(1L)))))));
        ElasticsearchAggregations aggregations = new ElasticsearchAggregations(Map.of("byOffers", aggregate));
        SearchHits<Product> searchHits = new SearchHitsImpl<>(1L, TotalHitsRelation.EQUAL_TO, 1f, Duration.ZERO, null, null,
                List.of(searchHit), aggregations, null, null);
        when(repository.search(any(NativeQuery.class), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(searchHits);

        Filter priceFilter = new Filter(FilterField.price.fieldPath(), FilterOperator.range, null, 100.0, 400.0);
        Filter conditionFilter = new Filter(FilterField.condition.fieldPath(), FilterOperator.term, List.of("NEW"), null,
                null);
        FilterRequestDto filters = new FilterRequestDto(List.of(priceFilter, conditionFilter), List.of());

        Agg aggregation = new Agg("byOffers", FilterField.offersCount.fieldPath(),
                AggType.terms, null, null, 5, null);
        AggregationRequestDto aggregationRequest = new AggregationRequestDto(List.of(aggregation));

        SearchService.SearchResult result = searchService.search(pageable, "electronics", "eco", aggregationRequest, filters);

        assertThat(result.hits().getSearchHits()).hasSize(1);
        assertThat(result.aggregations()).hasSize(1);
        assertThat(result.aggregations().get(0).buckets()).hasSize(1);
        assertThat(result.aggregations().get(0).buckets().get(0).count()).isEqualTo(1L);
    }

    @Test
    void defaultQueriesExcludeModeratedProducts() {
        Query query = searchService.buildProductSearchQuery(null, null, null, true);
        List<Query> filters = extractFilters(query);

        assertThat(filters).anySatisfy(filter -> assertThat(isTermWithValue(filter, EXCLUDED_FIELD, false)).isTrue());
    }

    @Test
    void overrideQueriesOmitDefaultExclusionAndKeepCustomFilters() {
        Filter excludedFilter = new Filter(FilterField.excludedCauses.fieldPath(), FilterOperator.term,
                List.of("MODERATION"), null, null);
        FilterRequestDto normalized = searchService
                .normalizeFilters(new FilterRequestDto(List.of(excludedFilter), List.of()));

        Query query = searchService.buildProductSearchQuery(null, null, normalized, false);
        List<Query> filters = extractFilters(query);

        assertThat(filters).noneMatch(filter -> isTermWithValue(filter, EXCLUDED_FIELD, false));
        assertThat(filters).anySatisfy(filter -> assertThat(isTermsQueryWithValue(filter,
                FilterField.excludedCauses.fieldPath(), "MODERATION")).isTrue());
    }

    @Test
    void normalizeFiltersMergesRangesWithinMustClauses() {
        Filter lower = new Filter(FilterField.price.fieldPath(), FilterOperator.range, null, 0.0, null);
        Filter upper = new Filter(FilterField.price.fieldPath(), FilterOperator.range, null, null, 500.0);

        FilterRequestDto normalized = searchService
                .normalizeFilters(new FilterRequestDto(List.of(lower, upper), List.of()));

        assertThat(normalized.filters()).hasSize(1);
        Filter mergedRange = normalized.filters().getFirst();
        assertThat(mergedRange.min()).isEqualTo(0.0);
        assertThat(mergedRange.max()).isEqualTo(500.0);
    }

    @Test
    void shouldClausesRequireAtLeastOneMatch() {
        Filter lowerPrice = new Filter(FilterField.price.fieldPath(), FilterOperator.range, null, 0.0, 500.0);
        Filter midPrice = new Filter(FilterField.price.fieldPath(), FilterOperator.range, null, 500.0, 1000.0);
        FilterRequestDto filters = new FilterRequestDto(List.of(),
                List.of(new FilterRequestDto.FilterGroup(List.of(), List.of(lowerPrice, midPrice))));

        Query query = searchService.buildProductSearchQuery(null, null, filters, true);
        Optional<BoolQuery> groupQuery = extractFilters(query).stream()
                .map(this::asBool)
                .filter(Objects::nonNull)
                .filter(bool -> bool.should() != null && !bool.should().isEmpty())
                .findFirst();

        assertThat(groupQuery).isPresent();
        BoolQuery boolQuery = groupQuery.orElseThrow();
        assertThat(boolQuery.minimumShouldMatch()).isEqualTo("1");
        assertThat(boolQuery.should()).hasSize(2);
    }

    @Test
    void excludedAggregationUsesOverrideQueryWhenRequested() {
        Pageable pageable = PageRequest.of(0, 5);

        Aggregate visibleAggregate = Aggregate.of(a -> a.sterms(st -> st.buckets(b -> b.array(List.of()))));
        Aggregate overrideAggregate = Aggregate.of(a -> a.sterms(st -> st.buckets(b -> b.array(List.of(
                StringTermsBucket.of(bucket -> bucket.key(FieldValue.of(fv -> fv.stringValue("MODERATION"))).docCount(2L)),
                StringTermsBucket.of(bucket -> bucket.key(FieldValue.of(fv -> fv.stringValue("LEGAL"))).docCount(3L))
        )))));

        SearchHits<Product> mainHits = buildSearchHits(List.of(new Product(5L)),
                Map.of("excludedStats", visibleAggregate));
        SearchHits<Product> overrideHits = buildSearchHits(List.of(), Map.of("excludedStats", overrideAggregate));

        when(repository.search(any(NativeQuery.class), eq(ProductRepository.MAIN_INDEX_NAME)))
                .thenReturn(mainHits, overrideHits);

        Agg aggregation = new Agg("excludedStats", FilterField.excludedCauses.fieldPath(), AggType.terms, null, null, null,
                null);
        AggregationRequestDto aggregationRequest = new AggregationRequestDto(List.of(aggregation));

        SearchService.SearchResult result = searchService.search(pageable, null, null, aggregationRequest, null);

        assertThat(result.aggregations()).hasSize(1);
        List<AggregationBucketDto> buckets = result.aggregations().getFirst().buckets();
        assertThat(buckets).extracting(AggregationBucketDto::key).contains("MODERATION", "LEGAL");

        ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(repository, times(2)).search(queryCaptor.capture(), eq(ProductRepository.MAIN_INDEX_NAME));
        List<NativeQuery> executed = queryCaptor.getAllValues();

        assertThat(extractFilters(executed.get(0).getQuery()))
                .anyMatch(filter -> isTermWithValue(filter, EXCLUDED_FIELD, false));
        assertThat(extractFilters(executed.get(1).getQuery()))
                .noneMatch(filter -> isTermWithValue(filter, EXCLUDED_FIELD, false));
    }

    @Test
    void searchFallsBackToSemanticWhenNoHits() {
        Pageable pageable = PageRequest.of(0, 10);
        when(verticalsConfigService.getConfigById("electronics")).thenReturn(new VerticalConfig());
        when(textEmbeddingService.embed(any())).thenReturn(new float[] { 0.1f, 0.2f });

        SearchHits<Product> emptyHits = buildSearchHits(List.of());
        SearchHits<Product> semanticHits = buildSearchHits(List.of(new Product(42L)));

        when(repository.search(any(NativeQuery.class), eq(ProductRepository.MAIN_INDEX_NAME)))
                .thenReturn(emptyHits, semanticHits);

        SearchService.SearchResult result = searchService.search(pageable, "electronics", "eco", null, null);

        assertThat(result.hits().getSearchHits()).hasSize(1);
        assertThat(result.aggregations()).isEmpty();
        verify(repository, times(2)).search(any(NativeQuery.class), eq(ProductRepository.MAIN_INDEX_NAME));
    }

    @Test
    void globalSearchFallsBackToSemanticAfterTextFallback() {
        VerticalConfig config = new VerticalConfig();
        config.setId("smartphones");
        ProductI18nElements elements = new ProductI18nElements();
        elements.setVerticalHomeTitle("Smartphones");
        elements.setVerticalHomeUrl("/smartphones");
        Map<String, ProductI18nElements> i18n = new HashMap<>();
        i18n.put("fr", elements);
        config.setI18n(i18n);

        when(verticalsConfigService.getConfigsWithoutDefault()).thenReturn(List.of(config));
        searchService.initializeSuggestIndex();

        when(textEmbeddingService.embed(any())).thenReturn(new float[] { 0.1f, 0.2f });

        Product semanticProduct = new Product(12L);
        semanticProduct.setVertical("smartphones");
        SearchHits<Product> emptyHits = buildSearchHits(List.of());
        SearchHits<Product> semanticHits = buildSearchHits(List.of(semanticProduct));

        when(repository.search(any(NativeQuery.class), eq(ProductRepository.MAIN_INDEX_NAME)))
                .thenReturn(emptyHits, emptyHits, semanticHits);

        ProductDto productDto = new ProductDto(0L, null, null, null, null, null, null, null, null, null, null, null, null, null);
        when(productMappingService.mapProduct(eq(semanticProduct), any(), any(), eq(DomainLanguage.fr), eq(false)))
                .thenReturn(productDto);

        SearchService.GlobalSearchResult result = searchService.globalSearch("smartphone", DomainLanguage.fr);

        assertThat(result.fallbackTriggered()).isTrue();
        assertThat(result.verticalGroups()).isEmpty();
        assertThat(result.fallbackResults()).hasSize(1);
        verify(repository, times(3)).search(any(NativeQuery.class), eq(ProductRepository.MAIN_INDEX_NAME));
    }

    private List<Query> extractFilters(Query query) {
        BoolQuery bool = asBool(query);
        if (bool == null || bool.filter() == null) {
            return List.of();
        }
        return bool.filter();
    }

    private BoolQuery asBool(Query query) {
        if (query == null || !query.isBool()) {
            return null;
        }
        return query.bool();
    }

    private boolean isTermWithValue(Query query, String field, Object expected) {
        if (query == null || !query.isTerm()) {
            return false;
        }
        TermQuery term = query.term();
        if (!field.equals(term.field())) {
            return false;
        }
        FieldValue value = term.value();
        return matchesFieldValue(value, expected);
    }

    private boolean isTermsQueryWithValue(Query query, String field, Object expected) {
        if (query == null || !query.isTerms()) {
            return false;
        }
        TermsQuery terms = query.terms();
        if (!field.equals(terms.field()) || terms.terms() == null) {
            return false;
        }
        List<FieldValue> values = terms.terms().value();
        if (values == null) {
            return false;
        }
        return values.stream().anyMatch(value -> matchesFieldValue(value, expected));
    }

    private boolean matchesFieldValue(FieldValue value, Object expected) {
        if (expected instanceof Boolean booleanExpected && value.isBoolean()) {
            return value.booleanValue() == booleanExpected;
        }
        if (expected instanceof Number number) {
            if (value.isDouble()) {
                return Objects.equals(value.doubleValue(), number.doubleValue());
            }
            if (value.isLong()) {
                return Objects.equals(value.longValue(), number.longValue());
            }
        }
        if (value.isString()) {
            return Objects.equals(value.stringValue(), String.valueOf(expected));
        }
        if (value.isAny()) {
            return Objects.equals(value.anyValue(), expected);
        }
        return false;
    }

    private SearchHits<Product> buildSearchHits(List<Product> products) {
        return buildSearchHits(products, null);
    }

    private SearchHits<Product> buildSearchHits(List<Product> products, Map<String, Aggregate> aggregationMap) {
        List<SearchHit<Product>> hits = products.stream()
                .map(product -> new SearchHit<>(ProductRepository.MAIN_INDEX_NAME,
                        product.getId() == null ? "0" : product.getId().toString(), null, 1f, null, Map.of(), Map.of(), null,
                        null, List.of(), product))
                .toList();
        ElasticsearchAggregations aggregations = aggregationMap == null ? null : new ElasticsearchAggregations(aggregationMap);
        return new SearchHitsImpl<>(hits.size(), TotalHitsRelation.EQUAL_TO, 1f, Duration.ZERO,
                null, null, hits, aggregations, null, null);
    }
}
