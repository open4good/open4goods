package org.open4goods.nudgerfrontapi.service;

import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.nudgerfrontapi.dto.ProductViewRequest;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.stereotype.Service;

/**
 * Default {@link ProductViewService} implementation. It retrieves the product
 * from {@link ProductRepository} and builds a {@link ProductViewResponse} used
 * by the frontend.
 */
@Service
public class ProductViewServiceImpl implements ProductViewService {

    private final ProductRepository productRepository;

    public ProductViewServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public ProductViewResponse render(ProductViewRequest productViewRequest) {
        try {
            Product product = productRepository.getById(productViewRequest.gtin());
            long gtin = Long.parseLong(product.gtin());
            return new ProductViewResponse(productViewRequest, null, gtin);
        } catch (ResourceNotFoundException e) {
            // Return an empty view if product does not exist
            return new ProductViewResponse(productViewRequest);
        }
    }
}
