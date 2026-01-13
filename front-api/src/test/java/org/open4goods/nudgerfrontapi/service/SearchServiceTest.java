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
import org.open4goods.nudgerfrontapi.dto.search.SearchMode;
import org.open4goods.nudgerfrontapi.dto.search.SearchType;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.SearchService.GlobalSearchResult;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.data.domain.PageImpl;
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
                textEmbeddingService, embeddingProperties);
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

        // Mock empty results for product searches to avoid NPEs logic down the line if it tries to map hits
        // But here we return early on vertical match? No, findExactVerticalMatch just sets validity.
        // The service continues to execute searches.
        // Sequence: exact_vertical -> semantic -> global.

        // For this test, we just want to check CTA.
        SearchHits<Product> emptyHits = new SearchHitsImpl<Product>(0L, TotalHitsRelation.EQUAL_TO, 0.0f, java.time.Duration.ZERO, null, null, java.util.Collections.emptyList(), null, null, null);
        when(repository.search(any(), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(emptyHits);

        // WHEN
        GlobalSearchResult result = searchService.globalSearch("téléviseurs", DomainLanguage.fr, SearchType.global);

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
        GlobalSearchResult result = searchService.globalSearch("something else", DomainLanguage.fr, SearchType.global);

        // THEN
        assertThat(result.verticalCta()).isNull();
    }

    @Test
    void globalSearch_shouldFallbackToSemantic_whenExactVerticalFails() {
        // GIVEN initial setup
        when(verticalsConfigService.getConfigsWithoutDefault()).thenReturn(Collections.emptyList());
        searchService.initializeSuggestIndex();

        SearchHits<Product> emptyHits = new SearchHitsImpl<Product>(0L, TotalHitsRelation.EQUAL_TO, 0.0f, java.time.Duration.ZERO, null, null, java.util.Collections.emptyList(), null, null, null);

        // Setup repository to return empty for first call (exact_vertical)
        // And then we expect a call for semantic (which involves embedding)
        // Then we can assume semantic returns something or empty.

        when(repository.search(any(), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(emptyHits);

        // For semantic search, we need textEmbeddingService to return a vector
        when(textEmbeddingService.embed(any())).thenReturn(new float[]{0.1f, 0.2f});

        // WHEN
        GlobalSearchResult result = searchService.globalSearch("iphone", DomainLanguage.fr, SearchType.auto);

        // THEN
        // We verify that textEmbeddingService was called, which implies semantic search was attempted.
        // Since we return empty hits for everything, it should go: Exact -> Semantic -> Global
        verify(textEmbeddingService, times(1)).embed("iphone");

        // We can also check that the result has mode Global (since all failed/empty) or Semantic if we mocked hits?
        // If all fail/empty, it returns the start mode but with empty lists.
        // Actually the logic is: loops through sequence. If hits found, returns with that mode.
        // If loop finishes, returns empty result with startMode.
    }
}
