package org.open4goods.store.repository.elastic;

import org.open4goods.model.data.Brand;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface  BrandRepository extends ElasticsearchRepository<Brand, String> {

}
