package org.open4goods.commons.store.repository.elastic;

import org.open4goods.commons.model.TopPage;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface  VerticalPagesRepository extends ElasticsearchRepository<TopPage, String> {
	
}
