package org.open4goods.b2bapi.repository;

import java.util.Optional;
import java.util.UUID;
import org.open4goods.b2bapi.model.StripeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Stripe webhook idempotency records.
 */
@Repository
public interface StripeEventRepository extends JpaRepository<StripeEvent, UUID> {

    Optional<StripeEvent> findByStripeEventId(String stripeEventId);
}
