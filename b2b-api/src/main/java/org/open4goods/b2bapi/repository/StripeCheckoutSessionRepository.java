package org.open4goods.b2bapi.repository;

import java.util.Optional;
import java.util.UUID;
import org.open4goods.b2bapi.model.StripeCheckoutSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Stripe checkout session mirrors.
 */
@Repository
public interface StripeCheckoutSessionRepository extends JpaRepository<StripeCheckoutSession, UUID> {

    Optional<StripeCheckoutSession> findByStripeSessionId(String stripeSessionId);
}
