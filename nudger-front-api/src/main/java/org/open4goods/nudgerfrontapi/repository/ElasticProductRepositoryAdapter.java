package org.open4goods.nudgerfrontapi.repository;

import java.util.Optional;

import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.stereotype.Service;

/**
 * Adapter delegating calls to {@link ProductRepository}.
 */
@Service
public class ElasticProductRepositoryAdapter implements ProductRepositoryPort {

    private final ProductRepository productRepository;

    public ElasticProductRepositoryAdapter(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Optional<Product> findByGtin(long gtin) {
        try {
            return Optional.ofNullable(productRepository.getById(gtin));
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
    }
}
