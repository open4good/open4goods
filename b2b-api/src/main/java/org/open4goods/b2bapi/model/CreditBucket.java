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
 * Authoritative mutable credit balance bucket.
 */
@Entity
@Table(name = "credit_buckets")
public class CreditBucket extends BaseUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private CreditBucketKind kind;

    @Column(nullable = false)
    private long creditsTotal;

    @Column(nullable = false)
    private long creditsRemaining;

    private Instant expiresAt;

    @Column(columnDefinition = "text")
    private String catalogId;

    @Column(columnDefinition = "text")
    private String sourceRef;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected CreditBucket() {
    }

    public CreditBucket(final Organization organization, final CreditBucketKind kind,
            final long creditsTotal, final long creditsRemaining) {
        this.organization = organization;
        this.kind = kind;
        this.creditsTotal = creditsTotal;
        this.creditsRemaining = creditsRemaining;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(final Organization organization) {
        this.organization = organization;
    }

    public CreditBucketKind getKind() {
        return kind;
    }

    public void setKind(final CreditBucketKind kind) {
        this.kind = kind;
    }

    public long getCreditsTotal() {
        return creditsTotal;
    }

    public void setCreditsTotal(final long creditsTotal) {
        this.creditsTotal = creditsTotal;
    }

    public long getCreditsRemaining() {
        return creditsRemaining;
    }

    public void setCreditsRemaining(final long creditsRemaining) {
        this.creditsRemaining = creditsRemaining;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(final Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(final String catalogId) {
        this.catalogId = catalogId;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(final String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }
}
