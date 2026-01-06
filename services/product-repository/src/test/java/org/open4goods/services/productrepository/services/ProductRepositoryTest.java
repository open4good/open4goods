package org.open4goods.services.productrepository.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch._types.KnnSearch;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.product.Product;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductRepositoryTest
{

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private SearchHits<Product> searchHits;

    private ProductRepository repository;

    @BeforeEach
    void setUp()
    {
        repository = new ProductRepository();
        ReflectionTestUtils.setField(repository, "elasticsearchOperations", elasticsearchOperations);
    }

    @Test
    void computeMissingIdsUsesStringKeysFromExistingResults()
    {
        Map<String, Product> cachedResults = new HashMap<>();
        Product cachedProduct = new Product();
        cachedProduct.setId(123L);
        cachedResults.put("123", cachedProduct);

        Set<String> missingIds = ProductRepository.computeMissingIds(List.of(123L, 456L), cachedResults);

        assertThat(missingIds).containsExactlyInAnyOrder("456");
    }

    @Test
    void knnSearchByEmbeddingAddsGuardrailsToQuery()
    {
        when(elasticsearchOperations.search(any(org.springframework.data.elasticsearch.core.query.Query.class), eq(Product.class), eq(ProductRepository.CURRENT_INDEX))).thenReturn(searchHits);

        Criteria baseCriteria = new Criteria("vertical").is("phones");
        repository.knnSearchByEmbedding(new float[] {0.1f, 0.2f}, baseCriteria, 3);

        ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations).search(queryCaptor.capture(), eq(Product.class), eq(ProductRepository.CURRENT_INDEX));

        NativeQuery submittedQuery = queryCaptor.getValue();
        KnnSearch knnSearch = submittedQuery.getKnnSearches().getFirst();

        assertThat(knnSearch.k()).isEqualTo(3);
        assertThat(knnSearch.queryVector()).containsExactly(0.1f, 0.2f);
        assertThat(knnSearch.filter()).isEmpty();
        assertThat(submittedQuery.getPageable().getPageSize()).isEqualTo(3);
    }

    @Test
    void knnSearchByEmbeddingRejectsEmptyVectors()
    {
        assertThatThrownBy(() -> repository.knnSearchByEmbedding(new float[0], new Criteria("vertical"), 2))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Embedding vector must not be empty");
    }
}
