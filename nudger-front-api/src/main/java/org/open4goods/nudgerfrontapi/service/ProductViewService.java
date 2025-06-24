package org.open4goods.nudgerfrontapi.service;

import org.open4goods.nudgerfrontapi.dto.ProductViewRequest;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;

/**
 * Service responsible for producing a view of a product for the frontend.
 * Implementations fetch the product data and apply any transformation needed
 * for presentation.
 */
public interface ProductViewService {

    /**
     * Render a product view from the given request.
     *
     * @param productViewRequest parameters describing the product to render
     * @return the rendered product view
     */
    ProductViewResponse render(ProductViewRequest productViewRequest);
}
