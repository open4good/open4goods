package org.open4goods.b2bapi.repository;

import java.util.Optional;
import java.util.UUID;
import org.open4goods.b2bapi.model.StripeSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Stripe subscription mirrors.
 */
@Repository
public interface StripeSubscriptionRepository extends JpaRepository<StripeSubscription, UUID> {

    Optional<StripeSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}
