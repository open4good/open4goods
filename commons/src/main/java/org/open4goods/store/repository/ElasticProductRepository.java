package org.open4goods.store.repository;

import org.open4goods.model.product.Product;
import org.springframework.data.repository.CrudRepository;

/**
 * 
 * @author goulven
 *
 */

public interface  ElasticProductRepository extends CrudRepository<Product, String> {



}
