package org.open4goods.b2bapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Stripe checkout session correlation state.
 */
@Entity
@Table(name = "stripe_checkout_sessions")
public class StripeCheckoutSession extends BaseUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String stripeSessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private StripeCheckoutMode mode;

    @Column(nullable = false, columnDefinition = "text")
    private String catalogId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private StripeCheckoutStatus status = StripeCheckoutStatus.OPEN;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected StripeCheckoutSession() {
    }

    public StripeCheckoutSession(final Organization organization, final String stripeSessionId,
            final StripeCheckoutMode mode, final String catalogId) {
        this.organization = organization;
        this.stripeSessionId = stripeSessionId;
        this.mode = mode;
        this.catalogId = catalogId;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(final Organization organization) {
        this.organization = organization;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(final String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public StripeCheckoutMode getMode() {
        return mode;
    }

    public void setMode(final StripeCheckoutMode mode) {
        this.mode = mode;
    }

    public String getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(final String catalogId) {
        this.catalogId = catalogId;
    }

    public StripeCheckoutStatus getStatus() {
        return status;
    }

    public void setStatus(final StripeCheckoutStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }
}
