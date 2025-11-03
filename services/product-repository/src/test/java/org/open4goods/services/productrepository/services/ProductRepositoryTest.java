package org.open4goods.services.productrepository.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.model.product.Product;

class ProductRepositoryTest {

    @Test
    void computeMissingIdsUsesStringKeysFromExistingResults() {
        Map<String, Product> cachedResults = new HashMap<>();
        Product cachedProduct = new Product();
        cachedProduct.setId(123L);
        cachedResults.put("123", cachedProduct);

        Set<String> missingIds = ProductRepository.computeMissingIds(List.of(123L, 456L), cachedResults);

        assertThat(missingIds).containsExactlyInAnyOrder("456");
    }
}
