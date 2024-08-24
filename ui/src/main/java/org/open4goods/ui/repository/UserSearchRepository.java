package org.open4goods.ui.repository;

import org.open4goods.commons.model.data.GlobalUserSearch;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface  UserSearchRepository extends ElasticsearchRepository<GlobalUserSearch, String> {

}
