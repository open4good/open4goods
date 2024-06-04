package org.open4goods.store.repository.elastic;

import org.open4goods.model.data.BrandScore;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface  BrandScoresRepository extends ElasticsearchRepository<BrandScore, String> {

}
