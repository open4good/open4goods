package org.open4goods.nudgerfrontapi.service.mapper;

import org.open4goods.model.product.Product;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;

/**
 * Converts {@link Product} domain objects to frontend view DTOs.
 */
public class ProductViewMapper {

    /**
     * Map a {@link Product} to a {@link ProductViewResponse}.
     *
     * @param product the domain product
     * @return the mapped view DTO
     */
    public ProductViewResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }
        ProductViewResponse response = new ProductViewResponse(null);
        response.setGtin(product.getId());
        return response;
    }
}
