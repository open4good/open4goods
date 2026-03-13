package org.open4goods.services.productalert.repository;

import java.util.List;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.services.productalert.model.ProductAlertSubscription;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Repository for product-alert subscriptions.
 */
public interface ProductAlertSubscriptionRepository extends ElasticsearchRepository<ProductAlertSubscription, String>
{
    /**
     * Finds active subscriptions matching a GTIN and product condition.
     *
     * @param gtin numeric GTIN
     * @param condition product condition
     * @return matching subscriptions
     */
    List<ProductAlertSubscription> findByEnabledTrueAndGtinAndCondition(Long gtin, ProductCondition condition);
}
