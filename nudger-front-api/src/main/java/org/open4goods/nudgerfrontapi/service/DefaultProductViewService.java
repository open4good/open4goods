package org.open4goods.nudgerfrontapi.service;

import org.open4goods.nudgerfrontapi.dto.ProductViewRequest;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link ProductViewUseCase} using the
 * {@link ProductRepository} to retrieve product data.
 */
@Service
public class DefaultProductViewService implements ProductViewUseCase {

    private final ProductRepository repo;

    public DefaultProductViewService(ProductRepository repo) {
        this.repo = repo;
    }

    @Override
    public ProductViewResponse render(ProductViewRequest productViewRequest) {
        ProductViewResponse response = new ProductViewResponse(productViewRequest);
        // TODO: fetch in repository
        // TODO: transform
        return response;
    }
}
