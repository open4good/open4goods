package org.open4goods.store.repository;

import org.open4goods.model.data.DataFragment;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface  DataFragmentRepository extends ElasticsearchRepository<DataFragment, String>, CustomDataFragmentRepository {





}
