package org.open4goods.services.productalert.repository;

import org.open4goods.services.productalert.model.ProductAlertNotificationCandidate;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Repository for notification candidates.
 */
public interface ProductAlertNotificationCandidateRepository extends ElasticsearchRepository<ProductAlertNotificationCandidate, String>
{
}
