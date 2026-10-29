package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.nudgerfrontapi.config.properties.OfferFreshnessProperties;
import org.open4goods.nudgerfrontapi.dto.search.AggregationBucketDto;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.SearchService.GlobalSearchResult;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;


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
        searchService = new SearchService(repository, verticalsConfigService, productMappingService, apiProperties,
                new OfferFreshnessService(new OfferFreshnessProperties()));
    }

    @Test
    void globalSearch_shouldReturnVerticalCta_whenQueryMatchesVerticalExactly() {
        // GIVEN
        VerticalConfig tvConfig = new VerticalConfig();
        tvConfig.setId("tv");
        tvConfig.setVerticalImage("tv.png");

        ProductI18nElements frElements = new ProductI18nElements();
        frElements.setVerticalHomeTitle("Téléviseurs");
        frElements.setVerticalHomeUrl("televiseurs");
        tvConfig.setI18n(Map.of("fr", frElements));

        when(verticalsConfigService.getConfigsWithoutDefault()).thenReturn(List.of(tvConfig));
        when(apiProperties.getResourceRootPath()).thenReturn("https://cdn.nudger.fr");

        searchService.initializeSuggestIndex();

        // Mock empty results for product searches while we focus on the CTA resolution.
        SearchHits<Product> emptyHits = new SearchHitsImpl<Product>(0L, TotalHitsRelation.EQUAL_TO, 0.0f, java.time.Duration.ZERO, null, null, java.util.Collections.emptyList(), null, null, null);
        when(repository.search(any(), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(emptyHits);

        // WHEN


        GlobalSearchResult result = searchService.globalSearch("téléviseurs", DomainLanguage.fr, null,
                org.springframework.data.domain.Sort.unsorted(), null);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.verticalCta()).isNotNull();
        assertThat(result.verticalCta().verticalId()).isEqualTo("tv");
        assertThat(result.verticalCta().verticalHomeTitle()).isEqualTo("Téléviseurs");
    }

    @Test
    void globalSearch_shouldNotReturnVerticalCta_whenQueryDoesNotMatchStrictly() {
        // GIVEN
        VerticalConfig tvConfig = new VerticalConfig();
        tvConfig.setId("tv");
        tvConfig.setI18n(Map.of("fr", new ProductI18nElements()));
        when(verticalsConfigService.getConfigsWithoutDefault()).thenReturn(List.of(tvConfig));
        when(apiProperties.getResourceRootPath()).thenReturn("https://cdn.nudger.fr");

        SearchHits<Product> emptyHits = new SearchHitsImpl<Product>(0L, TotalHitsRelation.EQUAL_TO, 0.0f, java.time.Duration.ZERO, null, null, java.util.Collections.emptyList(), null, null, null);
        when(repository.search(any(), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(emptyHits);

        searchService.initializeSuggestIndex();

        // WHEN


        GlobalSearchResult result = searchService.globalSearch("something else", DomainLanguage.fr, null,
                org.springframework.data.domain.Sort.unsorted(), null);

        // THEN
        assertThat(result.verticalCta()).isNull();
    }

    @Test
    void globalSearch_shouldBuildOnlyLexicalQueries_whenNoVerticalCandidates() {
        // GIVEN initial setup
        when(verticalsConfigService.getConfigsWithoutDefault()).thenReturn(Collections.emptyList());
        searchService.initializeSuggestIndex();

        SearchHits<Product> emptyHits = new SearchHitsImpl<Product>(0L, TotalHitsRelation.EQUAL_TO, 0.0f, java.time.Duration.ZERO, null, null, java.util.Collections.emptyList(), null, null, null);

        // Setup repository to return empty results
        when(repository.search(any(), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(emptyHits);

        // WHEN
        searchService.globalSearch("iphone", DomainLanguage.fr, null,
                org.springframework.data.domain.Sort.unsorted(), null);

        // THEN
        ArgumentCaptor<org.springframework.data.elasticsearch.core.query.Query> queryCaptor = ArgumentCaptor
                .forClass(org.springframework.data.elasticsearch.core.query.Query.class);
        verify(repository, atLeastOnce()).search(queryCaptor.capture(), eq(ProductRepository.MAIN_INDEX_NAME));
        assertThat(queryCaptor.getAllValues()).allSatisfy(query -> {
            NativeQuery nativeQuery = (NativeQuery) query;
            assertThat(nativeQuery.getKnnSearches()).isEmpty();
            assertThat(nativeQuery.getQuery().isBool()).isTrue();
        });
    }



    @Test
    @SuppressWarnings("unchecked")
    void suggest_shouldNotExposeStaleBestPrice() {
        long now = System.currentTimeMillis();
        Product freshProduct = new Product(1L);
        freshProduct.setPrice(new AggregatedPrices());
        freshProduct.getPrice().getOffers().add(OfferFreshnessServiceTest.offer("fnac", 12.0,
                ProductCondition.NEW, now - Duration.ofHours(1).toMillis()));
        Product staleProduct = new Product(2L);
        staleProduct.setPrice(new AggregatedPrices());
        AggregatedPrice stale = OfferFreshnessServiceTest.offer("rakuten", 7.0,
                ProductCondition.NEW, now - Duration.ofDays(28).toMillis());
        staleProduct.getPrice().getOffers().add(stale);
        // Precomputed at aggregation time from the stale offer.
        staleProduct.getPrice().setMinPrice(stale);

        SearchHits<Product> hits = mock(SearchHits.class);
        SearchHit<Product> freshHit = mock(SearchHit.class);
        SearchHit<Product> staleHit = mock(SearchHit.class);
        when(freshHit.getContent()).thenReturn(freshProduct);
        when(staleHit.getContent()).thenReturn(staleProduct);
        when(hits.isEmpty()).thenReturn(false);
        when(hits.getSearchHits()).thenReturn(List.of(freshHit, staleHit));
        when(repository.search(any(), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(hits);

        SearchService.SuggestResult result = searchService.suggest("livre", DomainLanguage.fr);

        assertThat(result.productMatches()).hasSize(2);
        assertThat(result.productMatches().get(0).bestPrice()).isEqualTo(12.0);
        assertThat(result.productMatches().get(1).bestPrice()).isNull();
        assertThat(result.productMatches().get(1).bestPriceCurrency()).isNull();
    }

    @Test
    void suggest_shouldExecuteOneLexicalProductQuery_whenNoProductsMatch() {
        SearchHits<Product> emptyHits = new SearchHitsImpl<Product>(0L, TotalHitsRelation.EQUAL_TO, 0.0f,
                java.time.Duration.ZERO, null, null, java.util.Collections.emptyList(), null, null, null);

        when(repository.search(any(), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(emptyHits);

        searchService.suggest("iphone", DomainLanguage.fr);

        ArgumentCaptor<org.springframework.data.elasticsearch.core.query.Query> queryCaptor = ArgumentCaptor
                .forClass(org.springframework.data.elasticsearch.core.query.Query.class);
        verify(repository, times(1)).search(queryCaptor.capture(), eq(ProductRepository.MAIN_INDEX_NAME));
        NativeQuery nativeQuery = (NativeQuery) queryCaptor.getValue();
        assertThat(nativeQuery.getKnnSearches()).isEmpty();
    }
    @Test
    void search_shouldUseFilterQuery_whenQueryIsEmpty() {
        // GIVEN
        SearchHits<Product> emptyHits = new SearchHitsImpl<Product>(0L, TotalHitsRelation.EQUAL_TO, 0.0f,
                java.time.Duration.ZERO, null, null, java.util.Collections.emptyList(), null, null, null);
        when(repository.search(any(), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(emptyHits);

        // WHEN
        searchService.search(org.springframework.data.domain.Pageable.unpaged(), null, null, null, null);

        // THEN
        ArgumentCaptor<org.springframework.data.elasticsearch.core.query.Query> queryCaptor = ArgumentCaptor.forClass(org.springframework.data.elasticsearch.core.query.Query.class);
        verify(repository).search(queryCaptor.capture(), eq(ProductRepository.MAIN_INDEX_NAME));

        org.springframework.data.elasticsearch.core.query.Query capturedQuery = queryCaptor.getValue();
        assertThat(capturedQuery).isInstanceOf(NativeQuery.class);
        NativeQuery nativeQuery = (NativeQuery) capturedQuery;

        Query esQuery = nativeQuery.getQuery();
        // After fix, it should NOT use FunctionScore query for empty input, but a BoolQuery directly
        assertThat(esQuery.isFunctionScore()).isFalse();
        assertThat(esQuery.isBool()).isTrue();
        assertThat(nativeQuery.getKnnSearches()).isEmpty();
        assertSearchUsesProductProjection(nativeQuery);
    }

    @Test
    void search_shouldUseMultiMatchQuery_whenQueryIsProvided() {
        // GIVEN
        SearchHits<Product> emptyHits = new SearchHitsImpl<Product>(0L, TotalHitsRelation.EQUAL_TO, 0.0f,
                java.time.Duration.ZERO, null, null, java.util.Collections.emptyList(), null, null, null);
        when(repository.search(any(), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(emptyHits);

        // WHEN
        searchService.search(org.springframework.data.domain.Pageable.unpaged(), null, "some query", null, null);

        // THEN
        ArgumentCaptor<org.springframework.data.elasticsearch.core.query.Query> queryCaptor = ArgumentCaptor.forClass(org.springframework.data.elasticsearch.core.query.Query.class);
        verify(repository).search(queryCaptor.capture(), eq(ProductRepository.MAIN_INDEX_NAME));

        org.springframework.data.elasticsearch.core.query.Query capturedQuery = queryCaptor.getValue();
        assertThat(capturedQuery).isInstanceOf(NativeQuery.class);
        NativeQuery nativeQuery = (NativeQuery) capturedQuery;

        Query esQuery = nativeQuery.getQuery();
        assertThat(esQuery.isBool()).isTrue();
        assertThat(esQuery.bool().must()).isNotEmpty();
        boolean hasMultiMatch = esQuery.bool().must().stream().anyMatch(Query::isMultiMatch);
        assertThat(hasMultiMatch).isTrue();
        assertThat(nativeQuery.getKnnSearches()).isEmpty();
        assertSearchUsesProductProjection(nativeQuery);
    }

    private void assertSearchUsesProductProjection(NativeQuery nativeQuery) {
        assertThat(nativeQuery.getSourceFilter()).isInstanceOf(FetchSourceFilter.class);
        FetchSourceFilter sourceFilter = (FetchSourceFilter) nativeQuery.getSourceFilter();
        assertThat(sourceFilter.fetchSource()).isTrue();
        assertThat(sourceFilter.getIncludes()).isNull();
        assertThat(sourceFilter.getExcludes()).contains("embedding", "resources.imageInfo.embedding");
    }

    @Test
    void buildSearchCapabilities_shouldIncludeAllowedGlobalAggregations_whenVerticalIsNull() {
        SearchService.SearchCapabilities capabilities = searchService.buildSearchCapabilities(null, DomainLanguage.fr, Collections.emptyList());

        assertThat(capabilities.allowedAggregations()).contains("gtinInfos.country");
    }

    @Test
    void resolveVerticalFields_shouldIncludeRelativValue_forImpactScores() {
        // GIVEN
        VerticalConfig config = new VerticalConfig();
        config.setId("test-vertical");
        config.setAvailableImpactScoreCriterias(List.of("ECOSCORE"));
        // Ensure attributes config is not null to avoid potential NPEs, though logic handles nulls
        config.setAttributesConfig(new org.open4goods.model.vertical.AttributesConfig());

        // WHEN
        org.open4goods.nudgerfrontapi.dto.product.ProductFieldOptionsResponse response = searchService.resolveVerticalFields(config, DomainLanguage.fr, Collections.emptyList());

        // THEN
        // Verify that the standard value mapping is present
        assertThat(response.impact()).anyMatch(field -> field.mapping().equals("scores.ECOSCORE.value"));
        // Verify that the relativ.value mapping is ALSO present
        assertThat(response.impact()).anyMatch(field -> field.mapping().equals("scores.ECOSCORE.relativ.value"));
    }

    @Test
    void resolveVerticalFields_shouldPlaceCountryInImpact_andMoneyFiltersLastInGlobal() {
        // GIVEN
        VerticalConfig config = new VerticalConfig();
        config.setId("tv");
        config.setMoneyFilters(List.of("USAGE_COST_YEAR"));

        AttributesConfig attributesConfig = new AttributesConfig();
        AttributeConfig usageCost = new AttributeConfig();
        usageCost.setKey("USAGE_COST_YEAR");
        usageCost.setFilteringType(AttributeType.NUMERIC);
        attributesConfig.setConfigs(List.of(usageCost));
        config.setAttributesConfig(attributesConfig);

        // Simulate the controller passing the two core global numeric fields.
        var price = new org.open4goods.nudgerfrontapi.dto.product.FieldMetadataDto(
                "price.minPrice.price", null, null, "numeric", null);
        var offersCount = new org.open4goods.nudgerfrontapi.dto.product.FieldMetadataDto(
                "offersCount", null, null, "numeric", null);

        // WHEN
        org.open4goods.nudgerfrontapi.dto.product.ProductFieldOptionsResponse response = searchService
                .resolveVerticalFields(config, DomainLanguage.fr, List.of(price, offersCount));

        // THEN: country sits in the impact group, never in global.
        assertThat(response.impact()).anyMatch(field -> "gtinInfos.country".equals(field.mapping()));
        assertThat(response.global()).noneMatch(field -> "gtinInfos.country".equals(field.mapping()));

        // THEN: the money filter is appended last in the global group.
        List<String> globalMappings = response.global().stream().map(field -> field.mapping()).toList();
        assertThat(globalMappings).containsExactly(
                "price.minPrice.price", "offersCount", "attributes.indexed.USAGE_COST_YEAR.numericValue");
    }

    @Test
    void resolveVerticalFields_shouldMapEcoFiltersToImpact_andNotToTechnical() {
        // GIVEN
        VerticalConfig config = new VerticalConfig();
        config.setId("tv");
        config.setEcoFilters(List.of("CLASSE_ENERGY"));
        config.setTechnicalFilters(List.of("DIAGONALE_POUCES"));
        config.setGlobalTechnicalFilters(List.of("WEIGHT"));

        AttributesConfig attributesConfig = new AttributesConfig();
        AttributeConfig energyClass = new AttributeConfig();
        energyClass.setKey("CLASSE_ENERGY");
        energyClass.setFilteringType(AttributeType.TEXT);

        AttributeConfig diagonal = new AttributeConfig();
        diagonal.setKey("DIAGONALE_POUCES");
        diagonal.setFilteringType(AttributeType.NUMERIC);

        AttributeConfig weight = new AttributeConfig();
        weight.setKey("WEIGHT");
        weight.setFilteringType(AttributeType.NUMERIC);

        attributesConfig.setConfigs(List.of(energyClass, diagonal, weight));
        config.setAttributesConfig(attributesConfig);

        // WHEN
        org.open4goods.nudgerfrontapi.dto.product.ProductFieldOptionsResponse response = searchService
                .resolveVerticalFields(config, DomainLanguage.fr, Collections.emptyList());

        // THEN
        Set<String> impactMappings = response.impact().stream()
                .map(field -> field.mapping())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> technicalMappings = response.technical().stream()
                .map(field -> field.mapping())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        assertThat(impactMappings).contains("attributes.indexed.CLASSE_ENERGY.value");
        assertThat(technicalMappings).contains("attributes.indexed.DIAGONALE_POUCES.numericValue");
        assertThat(technicalMappings).contains("attributes.indexed.WEIGHT.numericValue");
        assertThat(technicalMappings).doesNotContain("attributes.indexed.CLASSE_ENERGY.value");
    }

    @Test
    void rangeBoundsDerivation_shouldIgnoreMissingBucket() {
        List<AggregationBucketDto> buckets = List.of(
                new AggregationBucketDto("ES-UNKNOWN", null, 12L, true),
                new AggregationBucketDto("10.0", 20.0, 3L, false),
                new AggregationBucketDto("20.0", 30.0, 1L, false),
                new AggregationBucketDto("ES-UNKNOWN", null, 5L, true));

        assertThat(SearchService.firstNumericRangeBucketKey(buckets)).isEqualTo(10.0);
        assertThat(SearchService.lastNumericRangeBucketTo(buckets)).isEqualTo(30.0);
    }

    @Test
    void rangeBoundsDerivation_shouldReturnNullForOnlyMissingBuckets() {
        List<AggregationBucketDto> buckets = List.of(
                new AggregationBucketDto("ES-UNKNOWN", null, 12L, true));

        assertThat(SearchService.firstNumericRangeBucketKey(buckets)).isNull();
        assertThat(SearchService.lastNumericRangeBucketTo(buckets)).isNull();
    }

    @Test
    void search_shouldBoundPageSizeAndAggregations() {
        SearchHits<Product> emptyHits = new SearchHitsImpl<Product>(0L, TotalHitsRelation.EQUAL_TO, 0.0f,
                java.time.Duration.ZERO, null, null, java.util.Collections.emptyList(), null, null, null);
        when(repository.search(any(), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(emptyHits);
        java.util.List<org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto.Agg> aggs = new java.util.ArrayList<>();
        for (int i = 0; i < 40; i++) {
            aggs.add(new org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto.Agg("t" + i, "vertical",
                    org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto.AggType.terms, null, null,
                    Integer.MAX_VALUE, null));
        }

        searchService.search(org.springframework.data.domain.PageRequest.of(2, 100000), null, null,
                new org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto(aggs), null);

        ArgumentCaptor<org.springframework.data.elasticsearch.core.query.Query> captor = ArgumentCaptor
                .forClass(org.springframework.data.elasticsearch.core.query.Query.class);
        verify(repository).search(captor.capture(), eq(ProductRepository.MAIN_INDEX_NAME));
        NativeQuery nativeQuery = (NativeQuery) captor.getValue();
        assertThat(nativeQuery.getPageable().getPageSize()).isEqualTo(SearchService.MAX_PAGE_SIZE);
        assertThat(nativeQuery.getAggregations()).hasSize(SearchService.MAX_AGGREGATIONS);
        nativeQuery.getAggregations().values().forEach(a -> assertThat(a.terms().size())
                .isEqualTo(SearchService.MAX_AGG_BUCKETS));
    }

    @Test
    void search_shouldThrowExplicitExceptionWhenElasticsearchResponseTooLarge() {
        when(repository.search(any(), eq(ProductRepository.MAIN_INDEX_NAME))).thenThrow(
                new org.springframework.dao.DataAccessResourceFailureException("failed",
                        new java.io.IOException("entity content is too long [104862036] for the configured buffer limit [104857600]")));

        org.junit.jupiter.api.Assertions.assertThrows(
                org.open4goods.nudgerfrontapi.service.exception.SearchResponseTooLargeException.class,
                () -> searchService.search(org.springframework.data.domain.PageRequest.of(0, 20), null, null, null, null));
    }
}
