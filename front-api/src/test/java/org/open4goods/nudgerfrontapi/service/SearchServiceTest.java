package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.product.Product;
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
import org.open4goods.nudgerfrontapi.service.ProductMappingService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.Criteria.CriteriaEntry;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;

/**
 * Unit tests for {@link SearchService} focusing on filter application.
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private VerticalsConfigService verticalsConfigService;

    @Mock
    private ProductMappingService productMappingService;

    @Mock
    private ApiProperties apiProperties;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        lenient().when(apiProperties.getResourceRootPath()).thenReturn("https://assets.nudger.test");
        lenient().when(repository.getRecentPriceQuery())
                .thenAnswer(invocation -> new Criteria("offersCount").greaterThan(0));
        searchService = new SearchService(repository, verticalsConfigService, productMappingService, apiProperties);
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
        Filter conditionFilter = new Filter(FilterField.condition.fieldPath(), FilterOperator.term, List.of("NEW"), null, null);
        FilterRequestDto filters = new FilterRequestDto(List.of(priceFilter, conditionFilter));

        Agg aggregation = new Agg("byOffers", FilterField.offersCount.fieldPath(),
                AggType.terms, null, null, 5, null);
        AggregationRequestDto aggregationRequest = new AggregationRequestDto(List.of(aggregation));

        SearchService.SearchResult result = searchService.search(pageable, "electronics", "eco", aggregationRequest, filters);

        assertThat(result.hits().getSearchHits()).hasSize(1);
        assertThat(result.aggregations()).hasSize(1);
        assertThat(result.aggregations().get(0).buckets()).hasSize(1);
        assertThat(result.aggregations().get(0).buckets().get(0).count()).isEqualTo(1L);

        ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
        org.mockito.Mockito.verify(repository).search(queryCaptor.capture(), eq(ProductRepository.MAIN_INDEX_NAME));
        NativeQuery builtQuery = queryCaptor.getValue();
        Criteria builtCriteria = ((CriteriaQuery) builtQuery.getSpringDataQuery()).getCriteria();
        var fieldNames = builtCriteria.getCriteriaChain().stream()
                .map(criteria -> criteria.getField() == null ? null : criteria.getField().getName())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        assertThat(fieldNames).contains("price.minPrice.price");
        assertThat(fieldNames).contains("price.conditions");
    }

    @Test
    void searchAllowsExplicitExcludedFilterToOverrideDefaultBehaviour() {
        Pageable pageable = PageRequest.of(0, 10);

        SearchHits<Product> emptyHits = buildSearchHits(List.of());
        Product excludedProduct = new Product(2L);
        excludedProduct.setExcluded(true);
        excludedProduct.setId(2L);
        SearchHits<Product> excludedHits = buildSearchHits(List.of(excludedProduct));

        when(repository.search(any(NativeQuery.class), eq(ProductRepository.MAIN_INDEX_NAME)))
                .thenReturn(emptyHits, excludedHits);

        SearchService.SearchResult defaultResult = searchService.search(pageable, null, null, null, null);
        assertThat(defaultResult.hits().getSearchHits()).isEmpty();

        Filter excludedFilter = new Filter(FilterField.excluded.fieldPath(), FilterOperator.term, List.of("true"), null, null);
        FilterRequestDto adminFilters = new FilterRequestDto(List.of(excludedFilter));

        SearchService.SearchResult overriddenResult = searchService.search(pageable, null, null, null, adminFilters);
        assertThat(overriddenResult.hits().getSearchHits()).hasSize(1);

        ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
        org.mockito.Mockito.verify(repository, times(2)).search(queryCaptor.capture(), eq(ProductRepository.MAIN_INDEX_NAME));

        List<NativeQuery> capturedQueries = queryCaptor.getAllValues();
        Criteria defaultCriteria = ((CriteriaQuery) capturedQueries.get(0).getSpringDataQuery()).getCriteria();
        assertThat(hasExcludedClauseWithValue(defaultCriteria, false)).isTrue();

        Criteria overriddenCriteria = ((CriteriaQuery) capturedQueries.get(1).getSpringDataQuery()).getCriteria();
        assertThat(hasExcludedClauseWithValue(overriddenCriteria, false)).isFalse();
        assertThat(hasExcludedClauseWithValue(overriddenCriteria, true)).isTrue();
    }

    @Test
    void termsAggregationOnBooleanFieldUsesDedicatedMissingBucket() {
        Pageable pageable = PageRequest.of(0, 5);

        Product product = new Product(3L);
        SearchHit<Product> hit = new SearchHit<>(ProductRepository.MAIN_INDEX_NAME, "3", null, 1f, null,
                Map.of(), Map.of(), null, null, List.of(), product);

        Aggregate termsAggregate = Aggregate.of(a -> a.sterms(st -> st.buckets(b -> b.array(List.of(
                StringTermsBucket.of(bucket -> bucket.key(FieldValue.of(fv -> fv.stringValue("true"))).docCount(2L))
        )))));
        Aggregate missingAggregate = Aggregate.of(a -> a.missing(m -> m.docCount(1L)));
        ElasticsearchAggregations aggregations = new ElasticsearchAggregations(Map.of(
                "excludedStats", termsAggregate,
                "excludedStats_missing", missingAggregate));

        SearchHits<Product> hits = new SearchHitsImpl<>(1L, TotalHitsRelation.EQUAL_TO, 1f, Duration.ZERO, null, null,
                List.of(hit), aggregations, null, null);
        when(repository.search(any(NativeQuery.class), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(hits, hits);

        Agg aggregation = new Agg("excludedStats", FilterField.excluded.fieldPath(), AggType.terms, null, null, null, null);
        AggregationRequestDto aggregationRequest = new AggregationRequestDto(List.of(aggregation));

        SearchService.SearchResult result = searchService.search(pageable, null, null, aggregationRequest, null);

        assertThat(result.aggregations()).hasSize(1);
        List<AggregationBucketDto> buckets = result.aggregations().get(0).buckets();
        assertThat(buckets).extracting(AggregationBucketDto::key)
                .contains("true", "ES-UNKNOWN");
        assertThat(buckets).anySatisfy(bucket -> {
            if ("ES-UNKNOWN".equals(bucket.key())) {
                assertThat(bucket.missing()).isTrue();
                assertThat(bucket.count()).isEqualTo(1L);
            }
        });
    }

    @Test
    void excludedAggregationIgnoresDefaultVisibilityFilter() {
        Pageable pageable = PageRequest.of(0, 5);

        Aggregate visibleAggregate = Aggregate.of(a -> a.sterms(st -> st.buckets(b -> b.array(List.of(
                StringTermsBucket.of(bucket -> bucket.key(FieldValue.of(fv -> fv.stringValue("false"))).docCount(5L))
        )))));
        Aggregate overrideAggregate = Aggregate.of(a -> a.sterms(st -> st.buckets(b -> b.array(List.of(
                StringTermsBucket.of(bucket -> bucket.key(FieldValue.of(fv -> fv.stringValue("true"))).docCount(2L)),
                StringTermsBucket.of(bucket -> bucket.key(FieldValue.of(fv -> fv.stringValue("false"))).docCount(3L))
        )))));
        Aggregate missingAggregate = Aggregate.of(a -> a.missing(m -> m.docCount(0L)));

        SearchHits<Product> mainHits = buildSearchHits(List.of(new Product(5L)),
                Map.of("excludedStats", visibleAggregate, "excludedStats_missing", missingAggregate));
        SearchHits<Product> overrideHits = buildSearchHits(List.of(),
                Map.of("excludedStats", overrideAggregate, "excludedStats_missing", missingAggregate));

        when(repository.search(any(NativeQuery.class), eq(ProductRepository.MAIN_INDEX_NAME)))
                .thenReturn(mainHits, overrideHits);

        Agg aggregation = new Agg("excludedStats", FilterField.excluded.fieldPath(), AggType.terms, null, null, null, null);
        AggregationRequestDto aggregationRequest = new AggregationRequestDto(List.of(aggregation));

        SearchService.SearchResult result = searchService.search(pageable, null, null, aggregationRequest, null);

        assertThat(result.aggregations()).hasSize(1);
        List<AggregationBucketDto> buckets = result.aggregations().get(0).buckets();
        assertThat(buckets).extracting(AggregationBucketDto::key).contains("true", "false");
        assertThat(buckets).anySatisfy(bucket -> {
            if ("true".equals(bucket.key())) {
                assertThat(bucket.count()).isEqualTo(2L);
            }
        });

        ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
        org.mockito.Mockito.verify(repository, times(2)).search(queryCaptor.capture(), eq(ProductRepository.MAIN_INDEX_NAME));
        List<NativeQuery> executedQueries = queryCaptor.getAllValues();

        Criteria defaultCriteria = ((CriteriaQuery) executedQueries.get(0).getSpringDataQuery()).getCriteria();
        assertThat(hasExcludedClauseWithValue(defaultCriteria, false)).isTrue();

        Criteria adminCriteria = ((CriteriaQuery) executedQueries.get(1).getSpringDataQuery()).getCriteria();
        assertThat(hasExcludedClauseWithValue(adminCriteria, false)).isFalse();
        assertThat(hasExcludedClauseWithValue(adminCriteria, true)).isFalse();
    }

    private SearchHits<Product> buildSearchHits(List<Product> products) {
        return buildSearchHits(products, null);
    }

    private SearchHits<Product> buildSearchHits(List<Product> products, Map<String, Aggregate> aggregationMap) {
        List<SearchHit<Product>> hits = new ArrayList<>();
        for (Product product : products) {
            String documentId = product.getId() == null ? "0" : product.getId().toString();
            hits.add(new SearchHit<>(ProductRepository.MAIN_INDEX_NAME, documentId, null, 1f, null,
                    Map.of(), Map.of(), null, null, List.of(), product));
        }
        ElasticsearchAggregations aggregations = aggregationMap == null ? null : new ElasticsearchAggregations(aggregationMap);
        return new SearchHitsImpl<>(hits.size(), TotalHitsRelation.EQUAL_TO, 1f, Duration.ZERO,
                null, null, hits, aggregations, null, null);
    }

    private boolean hasExcludedClauseWithValue(Criteria criteria, boolean expectedValue) {
        return expandCriteria(criteria)
                .filter(entry -> entry.getField() != null && "excluded".equals(entry.getField().getName()))
                .flatMap(entry -> entry.getQueryCriteriaEntries().stream())
                .map(CriteriaEntry::getValue)
                .flatMap(this::flattenCriteriaValue)
                .anyMatch(value -> matchesExpectedValue(value, expectedValue));
    }

    private boolean matchesExpectedValue(Object value, boolean expectedValue) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue == expectedValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue) == expectedValue;
        }
        return Objects.equals(value, expectedValue);
    }

    private Stream<Criteria> expandCriteria(Criteria root) {
        if (root == null) {
            return Stream.empty();
        }
        Deque<Criteria> toVisit = new ArrayDeque<>();
        Set<Criteria> visited = new HashSet<>();
        List<Criteria> collected = new ArrayList<>();
        toVisit.push(root);
        while (!toVisit.isEmpty()) {
            Criteria current = toVisit.pop();
            if (!visited.add(current)) {
                continue;
            }
            collected.add(current);
            current.getCriteriaChain().forEach(toVisit::push);
            current.getSubCriteria().forEach(toVisit::push);
        }
        return collected.stream();
    }

    private Stream<?> flattenCriteriaValue(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream();
        }
        return Stream.ofNullable(value);
    }


}
