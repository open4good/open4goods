package org.open4goods.commons.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchServiceTest {

    @Mock
    private ProductRepository repository;

    private SearchService service;

    @BeforeEach
    void setUp() {
        service = new SearchService(repository, "");
    }

    @Test
    void searchProductsDelegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        SearchHits<Product> hits = SearchHits.empty();
        when(repository.search(any(Query.class), eq(ProductRepository.MAIN_INDEX_NAME))).thenReturn(hits);

        var result = service.searchProducts("abc", pageable);

        assertThat(result).isSameAs(hits);

        verify(repository).search(any(Query.class), eq(ProductRepository.MAIN_INDEX_NAME));
    }

    @Test
    void sanitizeRemovesSpecialCharacters() {
        assertThat(service.sanitize("a###b")).isEqualTo("a b");
    }
}
