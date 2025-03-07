package org.open4goods.commons.store.repository.elastic;

import org.open4goods.commons.model.Text;
import org.open4goods.model.product.Product;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 
 * @author goulven
 *
 */

public interface  ElasticTextRepository extends ElasticsearchRepository<Text, String> {



}
