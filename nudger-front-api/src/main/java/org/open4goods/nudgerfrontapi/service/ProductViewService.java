package org.open4goods.nudgerfrontapi.service;

import org.open4goods.nudgerfrontapi.dto.ProductViewRequest;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;

/**
 * Service interface for rendering a product view from a request.
 */
public interface ProductViewService {

    /**
     * Renders a product view from the given request.
     *
     * @param productViewRequest the view request
     * @return the rendered response
     */
    ProductViewResponse render(ProductViewRequest productViewRequest);
}
