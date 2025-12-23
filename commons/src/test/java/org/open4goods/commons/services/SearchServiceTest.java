package org.open4goods.commons.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.commons.model.dto.VerticalSearchRequest;
import org.open4goods.commons.model.dto.VerticalSearchResponse;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest
{

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TextEmbeddingService textEmbeddingService;

    @Mock
    private SearchHits<Product> searchHits;

    @Mock
    private SearchHit<Product> searchHit;

    private SearchService searchService;

    @BeforeEach
    void setUp()
    {
        searchService = new SearchService(productRepository, "logs", textEmbeddingService);
        lenient().when(productRepository.getRecentPriceQuery()).thenReturn(new Criteria("offersCount").greaterThan(0));
    }

    @Test
    void verticalSemanticSearchReturnsEmptyWhenEmbeddingMissing()
    {
        when(textEmbeddingService.embed("query")).thenReturn(null);

        VerticalSearchResponse response = searchService.verticalSemanticSearch(new VerticalConfig(), new VerticalSearchRequest(), "query");

        assertThat(response.getTotalResults()).isZero();
        assertThat(response.getData()).isEmpty();
        verifyNoInteractions(productRepository);
    }

    @Test
    void verticalSemanticSearchDelegatesToRepository()
    {
        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setId("phones");

        VerticalSearchRequest request = new VerticalSearchRequest();
        request.setPageNumber(0);
        request.setPageSize(1);

        when(textEmbeddingService.embed("query")).thenReturn(new float[] {0.3f, 0.4f});

        Product product = new Product();
        when(searchHit.getContent()).thenReturn(product);
        when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));
        when(searchHits.getTotalHits()).thenReturn(1L);

        ArgumentCaptor<Criteria> criteriaCaptor = ArgumentCaptor.forClass(Criteria.class);
        when(productRepository.knnSearchByEmbedding(any(), criteriaCaptor.capture(), anyInt())).thenReturn(searchHits);

        VerticalSearchResponse response = searchService.verticalSemanticSearch(verticalConfig, request, "query");

        assertThat(response.getData()).containsExactly(product);
        assertThat(response.getFrom()).isZero();
        assertThat(response.getTo()).isEqualTo(1);
        assertThat(response.getTotalResults()).isEqualTo(1L);

        Criteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.getCriteriaChain().stream().anyMatch(c -> "vertical".equals(c.getField().getName()))).isTrue();
        assertThat(criteria.getCriteriaChain().stream().anyMatch(c -> "offersCount".equals(c.getField().getName()))).isTrue();
    }
}
