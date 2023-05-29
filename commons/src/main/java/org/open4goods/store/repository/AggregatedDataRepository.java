package org.open4goods.store.repository;

import org.open4goods.model.product.Product;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * This "blank" repository is useless, but it is the model trhat allows allows "easy" schema recreation for verticals (see VerticalGeneration.createIndex)
 * @author goulven
 *
 */

public interface  AggregatedDataRepository extends ElasticsearchRepository<Product, String> {



}
