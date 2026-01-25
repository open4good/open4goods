package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.SearchService.GlobalSearchResult;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;

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
        when(textEmbeddingService.embed(any())).thenReturn(new float[]{0.1f});

        GlobalSearchResult result = searchService.globalSearch("téléviseurs", DomainLanguage.fr, null,
                org.springframework.data.domain.Sort.unsorted());

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
        when(textEmbeddingService.embed(any())).thenReturn(new float[]{0.1f});

        GlobalSearchResult result = searchService.globalSearch("something else", DomainLanguage.fr, null,
                org.springframework.data.domain.Sort.unsorted());

        // THEN
        assertThat(result.verticalCta()).isNull();
    }

    @Test
    void globalSearch_shouldPerformSemantic_whenNoVerticalCandidates() {
        // GIVEN initial setup
        when(verticalsConfigService.getConfigsWithoutDefault()).thenReturn(Collections.emptyList());
        searchService.initializeSuggestIndex();

        SearchHits<Product> emptyHits = new SearchHitsImpl<Product>(0L, TotalHitsRelation.EQUAL_TO, 0.0f, java.time.Duration.ZERO, null, null, java.util.Collections.emptyList(), null, null, null);

        // Setup repository to return empty semantic search results
        when(repository.search(any(), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(emptyHits);
        
        when(textEmbeddingService.embed(any())).thenReturn(new float[]{0.1f});



        // WHEN
        GlobalSearchResult result = searchService.globalSearch("iphone", DomainLanguage.fr, null,
                org.springframework.data.domain.Sort.unsorted());

        // THEN
        // We verify that semantic embeddings were requested once.
        verify(textEmbeddingService, times(1)).embed("iphone");
    }

    @Test
    void globalSearch_shouldExposeSemanticDiagnostics_whenEnabled() {
        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setId("bras");
        ProductI18nElements frElements = new ProductI18nElements();
        frElements.setVerticalHomeTitle("Bras articulés");
        frElements.setVerticalHomeUrl("bras-articules");
        verticalConfig.setI18n(Map.of("fr", frElements));

        when(verticalsConfigService.getConfigsWithoutDefault()).thenReturn(List.of(verticalConfig));
        when(apiProperties.getResourceRootPath()).thenReturn("https://cdn.nudger.fr");
        when(apiProperties.isSemanticDiagnosticsEnabled()).thenReturn(true);

        searchService.initializeSuggestIndex();

        SearchHits<Product> emptyHits = new SearchHitsImpl<Product>(0L, TotalHitsRelation.EQUAL_TO, 0.0f,
                java.time.Duration.ZERO, null, null, java.util.Collections.emptyList(), null, null, null);
        @SuppressWarnings("unchecked")
        SearchHits<Product> semanticHits = mock(SearchHits.class);
        @SuppressWarnings("unchecked")
        SearchHit<Product> searchHit = mock(SearchHit.class);
        Product product = new Product();

        when(semanticHits.isEmpty()).thenReturn(false);
        when(semanticHits.getSearchHits()).thenReturn(List.of(searchHit));
        when(searchHit.getContent()).thenReturn(product);
        when(searchHit.getScore()).thenReturn(1.2f);

        when(repository.search(any(), eq(ProductRepository.MAIN_INDEX_NAME)))
                .thenReturn(semanticHits, semanticHits);
        when(textEmbeddingService.embed(any())).thenReturn(new float[]{0.1f, 0.2f});
        when(productMappingService.mapProduct(any(), any(), any(), any(), eq(false)))
                .thenReturn(new ProductDto(0L, null, null, null, null, null, null, null, null, null, null, null, null, null));

        GlobalSearchResult result = searchService.globalSearch("bras articule", DomainLanguage.fr, null,
                org.springframework.data.domain.Sort.unsorted());

        assertThat(result).isNotNull();
        assertThat(result.diagnostics()).isNotNull();
        assertThat(result.diagnostics().resultCount()).isEqualTo(1);
        assertThat(result.diagnostics().topScore()).isCloseTo(1.2d, org.assertj.core.data.Offset.offset(0.0001d));
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
}
