package org.open4goods.store.repository.elastic;

import org.open4goods.model.product.Product;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 
 * @author goulven
 *
 */

public interface  ElasticProductRepository extends ElasticsearchRepository<Product, String> {



}
