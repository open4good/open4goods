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
 * Append-only credit ledger transaction.
 */
@Entity
@Table(name = "credit_transactions")
public class CreditTransaction extends BaseUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bucket_id")
    private CreditBucket bucket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private CreditTransactionType type;

    @Column(nullable = false)
    private long credits;

    @Column(columnDefinition = "text")
    private String facetId;

    @Column(columnDefinition = "text")
    private String gtin;

    @Column(columnDefinition = "text")
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actorUser;

    @Column(columnDefinition = "text")
    private String note;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected CreditTransaction() {
    }

    public CreditTransaction(final Organization organization, final CreditTransactionType type, final long credits) {
        this.organization = organization;
        this.type = type;
        this.credits = credits;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(final Organization organization) {
        this.organization = organization;
    }

    public CreditBucket getBucket() {
        return bucket;
    }

    public void setBucket(final CreditBucket bucket) {
        this.bucket = bucket;
    }

    public CreditTransactionType getType() {
        return type;
    }

    public void setType(final CreditTransactionType type) {
        this.type = type;
    }

    public long getCredits() {
        return credits;
    }

    public void setCredits(final long credits) {
        this.credits = credits;
    }

    public String getFacetId() {
        return facetId;
    }

    public void setFacetId(final String facetId) {
        this.facetId = facetId;
    }

    public String getGtin() {
        return gtin;
    }

    public void setGtin(final String gtin) {
        this.gtin = gtin;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(final String requestId) {
        this.requestId = requestId;
    }

    public User getActorUser() {
        return actorUser;
    }

    public void setActorUser(final User actorUser) {
        this.actorUser = actorUser;
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }
}
