package org.open4goods.b2bapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Stripe subscription mirror used to drive recurring credit grants.
 */
@Entity
@Table(name = "stripe_subscriptions")
public class StripeSubscription extends BaseUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String stripeSubscriptionId;

    @Column(nullable = false, columnDefinition = "text")
    private String catalogId;

    @Column(nullable = false, columnDefinition = "text")
    private String status;

    private Instant currentPeriodEnd;

    private Instant cancelAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected StripeSubscription() {
    }

    public StripeSubscription(final Organization organization, final String stripeSubscriptionId,
            final String catalogId, final String status) {
        this.organization = organization;
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.catalogId = catalogId;
        this.status = status;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(final Organization organization) {
        this.organization = organization;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public void setStripeSubscriptionId(final String stripeSubscriptionId) {
        this.stripeSubscriptionId = stripeSubscriptionId;
    }

    public String getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(final String catalogId) {
        this.catalogId = catalogId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public Instant getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public void setCurrentPeriodEnd(final Instant currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }

    public Instant getCancelAt() {
        return cancelAt;
    }

    public void setCancelAt(final Instant cancelAt) {
        this.cancelAt = cancelAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
