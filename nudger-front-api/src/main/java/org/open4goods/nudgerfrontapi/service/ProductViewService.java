package org.open4goods.nudgerfrontapi.service;

import org.open4goods.nudgerfrontapi.dto.ProductViewRequest;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;
import org.open4goods.nudgerfrontapi.repository.ProductRepositoryPort;
import org.springframework.stereotype.Service;

/**
 * Applies frontend transformation logic on products fetched from the repository.
 */
@Service
public class ProductViewService {

    private final ProductRepositoryPort productRepository;

    public ProductViewService(ProductRepositoryPort productRepository) {
        this.productRepository = productRepository;
    }

    public ProductViewResponse render(ProductViewRequest productViewRequest) {
        ProductViewResponse response = new ProductViewResponse(productViewRequest);
        // TODO: fetch from repository and transform
        productRepository.findByGtin(0); // placeholder usage
        return response;
    }
}
