package org.open4goods.services.productrepository.repository;

import org.open4goods.model.product.Product;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 
 * @author goulven
 *
 */

public interface  ElasticProductRepository extends ElasticsearchRepository<Product, Long> {



}
