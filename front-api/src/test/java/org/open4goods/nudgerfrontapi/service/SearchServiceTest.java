package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.open4goods.embedding.service.DjlTextEmbeddingService;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.nudgerfrontapi.config.properties.SearchProperties;

import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.SearchService.GlobalSearchResult;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;

import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.mockito.ArgumentCaptor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
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
    private SearchProperties searchProperties;
    @Mock
    private DjlTextEmbeddingService textEmbeddingService;
    @Mock
    private DjlEmbeddingProperties embeddingProperties;

    // Use a partial mock or spy if needed, but here we can stick to standard mocks
    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchProperties = new SearchProperties();
        searchService = new SearchService(repository, verticalsConfigService, productMappingService, apiProperties,
                searchProperties, textEmbeddingService, embeddingProperties);
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
                org.springframework.data.domain.Sort.unsorted(), null, null);

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
                org.springframework.data.domain.Sort.unsorted(), null, null);

        // THEN
        assertThat(result.verticalCta()).isNull();
    }

    @Test
    void globalSearch_shouldNeverPerformSemantic_evenWhenNoVerticalCandidates() {
        // GIVEN initial setup
        when(verticalsConfigService.getConfigsWithoutDefault()).thenReturn(Collections.emptyList());
        searchService.initializeSuggestIndex();

        SearchHits<Product> emptyHits = new SearchHitsImpl<Product>(0L, TotalHitsRelation.EQUAL_TO, 0.0f, java.time.Duration.ZERO, null, null, java.util.Collections.emptyList(), null, null, null);

        // Setup repository to return empty results
        when(repository.search(any(), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(emptyHits);
        
        // WHEN
        GlobalSearchResult result = searchService.globalSearch("iphone", DomainLanguage.fr, null,
                org.springframework.data.domain.Sort.unsorted(), null, null);

        // THEN
        // We verify that semantic embeddings were NEVER requested
        verify(textEmbeddingService, times(0)).embed(any());
    }



    @Test
    void suggest_shouldSkipSemanticFallback_whenDisabled() {
        searchProperties.getSuggest().setSemanticFallbackEnabled(false);
        SearchHits<Product> emptyHits = new SearchHitsImpl<Product>(0L, TotalHitsRelation.EQUAL_TO, 0.0f,
                java.time.Duration.ZERO, null, null, java.util.Collections.emptyList(), null, null, null);

        when(repository.search(any(), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(emptyHits);

        searchService.suggest("iphone", DomainLanguage.fr);

        verify(textEmbeddingService, times(0)).embed(any());
    }
    @Test
    void search_shouldUseFilterQuery_whenQueryIsEmpty() {
        // GIVEN
        SearchHits<Product> emptyHits = new SearchHitsImpl<Product>(0L, TotalHitsRelation.EQUAL_TO, 0.0f,
                java.time.Duration.ZERO, null, null, java.util.Collections.emptyList(), null, null, null);
        when(repository.search(any(), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(emptyHits);

        // WHEN
        searchService.search(org.springframework.data.domain.Pageable.unpaged(), null, null, null, null, false, null);

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
    }

    @Test
    void search_shouldUseMultiMatchQuery_whenQueryIsProvided() {
        // GIVEN
        SearchHits<Product> emptyHits = new SearchHitsImpl<Product>(0L, TotalHitsRelation.EQUAL_TO, 0.0f,
                java.time.Duration.ZERO, null, null, java.util.Collections.emptyList(), null, null, null);
        when(repository.search(any(), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(emptyHits);

        // WHEN
        searchService.search(org.springframework.data.domain.Pageable.unpaged(), null, "some query", null, null, false, null);

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
    }

    @Test
    void buildSearchCapabilities_shouldIncludeAllowedGlobalAggregations_whenVerticalIsNull() {
        SearchService.SearchCapabilities capabilities = searchService.buildSearchCapabilities(null, DomainLanguage.fr, Collections.emptyList());

        assertThat(capabilities.allowedAggregations()).contains("gtinInfos.country");
    }
}
