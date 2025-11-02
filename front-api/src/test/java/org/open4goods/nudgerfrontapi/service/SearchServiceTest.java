package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
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

    @Test
    void globalSearchReturnsGroupedResults() {
        when(repository.getRecentPriceQuery()).thenReturn(new Criteria("offersCount").greaterThan(0));
        when(repository.expirationClause()).thenReturn(123L);

        Product phone = new Product(1L);
        phone.setVertical("phones");
        phone.setOffersCount(5);
        phone.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, "Fairphone");
        phone.getAttributes().addReferentielAttribute(ReferentielKey.MODEL, "Fairphone 4");
        phone.getOfferNames().add("Fairphone 4 128 Go");

        Product laptop = new Product(2L);
        laptop.setVertical("laptops");
        laptop.setOffersCount(3);
        laptop.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, "Framework");
        laptop.getAttributes().addReferentielAttribute(ReferentielKey.MODEL, "Framework Laptop");
        laptop.getOfferNames().add("Framework Laptop 13");

        SearchHit<Product> phoneHit = new SearchHit<>(ProductRepository.MAIN_INDEX_NAME, "1", null, 5f, null,
                Map.of(), Map.of(), null, null, List.of(), phone);
        SearchHit<Product> laptopHit = new SearchHit<>(ProductRepository.MAIN_INDEX_NAME, "2", null, 3f, null,
                Map.of(), Map.of(), null, null, List.of(), laptop);
        SearchHits<Product> firstPassHits = mock(SearchHits.class);
        when(firstPassHits.isEmpty()).thenReturn(false);
        when(firstPassHits.getSearchHits()).thenReturn(List.of(phoneHit, laptopHit));
        when(repository.search(any(NativeQuery.class), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(firstPassHits);

        SearchService.GlobalSearchResult result = searchService.globalSearch("Fairphone", DomainLanguage.fr);

        assertThat(result.verticalGroups()).hasSize(2);
        assertThat(result.verticalGroups().get(0).verticalId()).isEqualTo("phones");
        assertThat(result.verticalGroups().get(0).results()).hasSize(1);
        assertThat(result.fallbackResults()).isEmpty();
        assertThat(result.fallbackTriggered()).isFalse();
    }

    @Test
    void globalSearchTriggersFallbackWhenNoFirstPassHits() {
        when(repository.getRecentPriceQuery()).thenReturn(new Criteria("offersCount").greaterThan(0));
        when(repository.expirationClause()).thenReturn(123L);

        SearchHits<Product> emptyHits = mock(SearchHits.class);
        when(emptyHits.isEmpty()).thenReturn(true);

        Product accessory = new Product(3L);
        accessory.setOffersCount(2);
        accessory.getOfferNames().add("Universal Charger");

        SearchHit<Product> fallbackHit = new SearchHit<>(ProductRepository.MAIN_INDEX_NAME, "3", null, 2f, null,
                Map.of(), Map.of(), null, null, List.of(), accessory);
        SearchHits<Product> fallbackHits = mock(SearchHits.class);
        when(fallbackHits.isEmpty()).thenReturn(false);
        when(fallbackHits.getSearchHits()).thenReturn(List.of(fallbackHit));

        when(repository.search(any(NativeQuery.class), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(emptyHits,
                fallbackHits);

        SearchService.GlobalSearchResult result = searchService.globalSearch("chargeur", DomainLanguage.en);

        assertThat(result.verticalGroups()).isEmpty();
        assertThat(result.fallbackResults()).hasSize(1);
        assertThat(result.fallbackResults().get(0).gtin()).isEqualTo(3L);
        assertThat(result.fallbackTriggered()).isTrue();
    }
}
