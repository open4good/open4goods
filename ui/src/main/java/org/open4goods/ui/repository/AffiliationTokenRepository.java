package org.open4goods.ui.repository;

import org.open4goods.model.data.AffiliationToken;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface  AffiliationTokenRepository extends ElasticsearchRepository<AffiliationToken, String> {

}
