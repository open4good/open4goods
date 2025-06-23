package org.open4goods.nudgerfrontapi.service;

import org.open4goods.model.product.Product;
import org.open4goods.nudgerfrontapi.dto.ProductViewRequest;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;
import org.open4goods.nudgerfrontapi.service.mapper.ProductViewMapper;

/**
 * Default implementation handling the product view transformation.
 */
public class DefaultProductViewService {

    private final ProductViewMapper mapper;

    public DefaultProductViewService(ProductViewMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Render the provided product using optional request parameters.
     *
     * @param productViewRequest details about the rendering request
     * @param product the product to render
     * @return a product view representation
     */
    public ProductViewResponse render(ProductViewRequest productViewRequest, Product product) {
        return mapper.toResponse(product);
    }
}
