package org.open4goods.commons.store.repository.elastic;

import org.open4goods.commons.model.product.Product;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 
 * @author goulven
 *
 */

public interface  ElasticProductRepository extends ElasticsearchRepository<Product, String> {



}
