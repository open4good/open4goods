package org.open4goods.nudgerfrontapi.repository;

import java.util.Optional;

import org.open4goods.model.product.Product;

/**
 * Port for product repository access used by the front API service.
 */
public interface ProductRepositoryPort {

    /**
     * Retrieves a product by its GTIN.
     *
     * @param gtin the product GTIN
     * @return optional product
     */
    Optional<Product> findByGtin(long gtin);
}
