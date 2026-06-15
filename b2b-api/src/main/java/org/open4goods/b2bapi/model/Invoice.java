package org.open4goods.b2bapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Customer-visible invoice and payment mirror.
 */
@Entity
@Table(name = "invoices")
public class Invoice extends BaseUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String stripeInvoiceId;

    @Column(nullable = false)
    private int amountCents;

    @Column(nullable = false, columnDefinition = "text")
    private String currency = "eur";

    @Column(nullable = false, columnDefinition = "text")
    private String status;

    @Column(columnDefinition = "text")
    private String hostedInvoiceUrl;

    private Long creditsGranted;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Invoice() {
    }

    public Invoice(final Organization organization, final String stripeInvoiceId,
            final int amountCents, final String status) {
        this.organization = organization;
        this.stripeInvoiceId = stripeInvoiceId;
        this.amountCents = amountCents;
        this.status = status;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(final Organization organization) {
        this.organization = organization;
    }

    public String getStripeInvoiceId() {
        return stripeInvoiceId;
    }

    public void setStripeInvoiceId(final String stripeInvoiceId) {
        this.stripeInvoiceId = stripeInvoiceId;
    }

    public int getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(final int amountCents) {
        this.amountCents = amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getHostedInvoiceUrl() {
        return hostedInvoiceUrl;
    }

    public void setHostedInvoiceUrl(final String hostedInvoiceUrl) {
        this.hostedInvoiceUrl = hostedInvoiceUrl;
    }

    public Long getCreditsGranted() {
        return creditsGranted;
    }

    public void setCreditsGranted(final Long creditsGranted) {
        this.creditsGranted = creditsGranted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }
}
