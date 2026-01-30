package org.open4goods.nudgerfrontapi.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

@ExtendWith(MockitoExtension.class)
class SearchServiceReproductionTest {

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

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchProperties = new SearchProperties();
        searchService = new SearchService(repository, verticalsConfigService, productMappingService, apiProperties,
                searchProperties, textEmbeddingService, embeddingProperties);
    }

    @Test
    void globalSearch_shouldApplyMissingVerticalFilter() {
        // GIVEN
        searchProperties.getSuggest().setSemanticFallbackEnabled(false);



        // Mock empty hits
        SearchHitsImpl<Product> emptyHits = new SearchHitsImpl<>(0L, TotalHitsRelation.EQUAL_TO, 0.0f,
                java.time.Duration.ZERO, null, null, java.util.Collections.emptyList(), null, null, null);
        when(repository.search(any(), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(emptyHits);

        searchService.initializeSuggestIndex();

        // WHEN
        searchService.globalSearch("bras", DomainLanguage.fr, null, null, "TEXT", null);

        // THEN
        ArgumentCaptor<org.springframework.data.elasticsearch.core.query.Query> queryCaptor = ArgumentCaptor.forClass(org.springframework.data.elasticsearch.core.query.Query.class);
        verify(repository, atLeastOnce()).search(queryCaptor.capture(), eq(ProductRepository.MAIN_INDEX_NAME));

        List<org.springframework.data.elasticsearch.core.query.Query> allQueries = queryCaptor.getAllValues();
        
        // We expect at least one query to have mustNot(exists("vertical"))
        boolean foundMissingVerticalQuery = false;
        
        for (org.springframework.data.elasticsearch.core.query.Query q : allQueries) {
            if (q instanceof NativeQuery nativeQuery) {
                Query esQuery = nativeQuery.getQuery();
                if (esQuery.isBool()) {
                   // Check for mustNot exists vertical
                   boolean hasMustNotExistingVertical = esQuery.bool().mustNot().stream()
                           .anyMatch(mq -> mq.isExists() && "vertical".equals(mq.exists().field()));
                   
                   if (hasMustNotExistingVertical) {
                       foundMissingVerticalQuery = true;
                   }
                }
            }
        }
        
        assertThat(foundMissingVerticalQuery).as("Should have triggered a query searching for items WITHOUT vertical").isTrue();
    }
}
