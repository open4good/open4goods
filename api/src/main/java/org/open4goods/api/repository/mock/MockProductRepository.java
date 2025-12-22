package org.open4goods.api.repository.mock;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.core.query.Query;

public class MockProductRepository extends ProductRepository {

    @Override
    public SearchHits<Product> search(Query query, String indexName) {
        return new SearchHitsImpl<Product>(
            0L,
            TotalHitsRelation.EQUAL_TO,
            0.0f,
            java.time.Duration.ZERO,
            null,
            null,
            java.util.Collections.emptyList(),
            null,
            null,
            null
        );
    }

    @Override
    public Product getById(Long productId) throws ResourceNotFoundException {
        throw new ResourceNotFoundException("Mock: Product " + productId + " not found (Local Mode)");
    }

    @Override
    public Long countMainIndex() {
        return 0L;
    }

    @Override
    public void index(Product product) {
        // No-op
    }
}
