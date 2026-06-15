package org.open4goods.b2bapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Per-request analytics event persisted from the Redis usage stream.
 */
@Entity
@Table(name = "usage_events")
public class UsageEvent extends BaseUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id")
    private ApiKey apiKey;

    @Column(nullable = false, columnDefinition = "text")
    private String facetId;

    @Column(columnDefinition = "text")
    private String gtin;

    @Column(nullable = false, columnDefinition = "text")
    private String requestId;

    @Column(nullable = false)
    private short httpStatus;

    @Column(nullable = false)
    private boolean billable;

    @Column(nullable = false)
    private long creditsConsumed;

    @Column(columnDefinition = "text")
    private String noPayReason;

    private Integer responseTimeMs;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected UsageEvent() {
    }

    public UsageEvent(final Organization organization, final String facetId,
            final String requestId, final short httpStatus, final boolean billable) {
        this.organization = organization;
        this.facetId = facetId;
        this.requestId = requestId;
        this.httpStatus = httpStatus;
        this.billable = billable;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(final Organization organization) {
        this.organization = organization;
    }

    public ApiKey getApiKey() {
        return apiKey;
    }

    public void setApiKey(final ApiKey apiKey) {
        this.apiKey = apiKey;
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

    public short getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(final short httpStatus) {
        this.httpStatus = httpStatus;
    }

    public boolean isBillable() {
        return billable;
    }

    public void setBillable(final boolean billable) {
        this.billable = billable;
    }

    public long getCreditsConsumed() {
        return creditsConsumed;
    }

    public void setCreditsConsumed(final long creditsConsumed) {
        this.creditsConsumed = creditsConsumed;
    }

    public String getNoPayReason() {
        return noPayReason;
    }

    public void setNoPayReason(final String noPayReason) {
        this.noPayReason = noPayReason;
    }

    public Integer getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(final Integer responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }
}
