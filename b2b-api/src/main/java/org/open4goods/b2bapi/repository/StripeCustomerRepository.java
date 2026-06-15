package org.open4goods.b2bapi.repository;

import java.util.Optional;
import java.util.UUID;
import org.open4goods.b2bapi.model.StripeCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Stripe customer mirrors.
 */
@Repository
public interface StripeCustomerRepository extends JpaRepository<StripeCustomer, UUID> {

    Optional<StripeCustomer> findByOrganizationId(UUID organizationId);

    Optional<StripeCustomer> findByStripeCustomerId(String stripeCustomerId);
}
