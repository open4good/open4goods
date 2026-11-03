package org.open4goods.services.contribution.repository;

import org.open4goods.services.contribution.model.AffiliationClick;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data repository of the outbound affiliation clicks ({@code affiliation-clicks} index). Its creation
 * creates the index with the {@link AffiliationClick} mapping when it does not exist.
 */
public interface AffiliationClickRepository extends ElasticsearchRepository<AffiliationClick, String> {
}
