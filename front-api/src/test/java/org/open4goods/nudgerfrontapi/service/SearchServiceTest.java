package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto.Agg;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto.AggType;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.Filter;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterField;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterOperator;
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
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
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

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(repository, verticalsConfigService);
    }

    @Test
    void searchAppliesFiltersAndKeepsAggregations() {
        Pageable pageable = PageRequest.of(0, 20);
        Criteria baseCriteria = new Criteria("offersCount").greaterThan(0);
        when(repository.getRecentPriceQuery()).thenReturn(baseCriteria);
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
}
