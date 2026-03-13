package org.open4goods.services.productalert.repository;

import org.open4goods.services.productalert.model.ProductAlertUser;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Repository for product-alert users.
 */
public interface ProductAlertUserRepository extends ElasticsearchRepository<ProductAlertUser, String>
{
}
