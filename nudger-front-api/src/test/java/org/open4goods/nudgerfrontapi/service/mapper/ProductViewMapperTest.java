package org.open4goods.nudgerfrontapi.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.open4goods.model.product.Product;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;

class ProductViewMapperTest {

    private final ProductViewMapper mapper = new ProductViewMapper();

    @Test
    void mapNullProductReturnsNull() {
        ProductViewResponse result = mapper.toResponse(null);
        assertNull(result);
    }

    @Test
    void mapCopiesGtin() {
        Product product = new Product(123L);
        ProductViewResponse result = mapper.toResponse(product);
        assertEquals(123L, result.getGtin());
    }
}
