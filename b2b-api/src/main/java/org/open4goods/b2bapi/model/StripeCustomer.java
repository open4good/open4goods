package org.open4goods.b2bapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Stripe customer mirror for an organization.
 */
@Entity
@Table(name = "stripe_customers")
public class StripeCustomer extends BaseUuidEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String stripeCustomerId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected StripeCustomer() {
    }

    public StripeCustomer(final Organization organization, final String stripeCustomerId) {
        this.organization = organization;
        this.stripeCustomerId = stripeCustomerId;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(final Organization organization) {
        this.organization = organization;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(final String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }
}
