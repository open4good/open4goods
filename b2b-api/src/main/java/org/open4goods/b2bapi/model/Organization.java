package org.open4goods.b2bapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Billable tenant owning credits, members, API keys, usage, and billing state.
 */
@Entity
@Table(name = "organizations")
public class Organization extends BaseUuidEntity {

    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String slug;

    @Column(columnDefinition = "text")
    private String billingEmail;

    @Column(nullable = false, columnDefinition = "text")
    private String defaultLanguage = "en";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    @Column(nullable = false)
    private boolean freeGrantApplied;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected Organization() {
    }

    public Organization(final String name, final String slug) {
        this.name = name;
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(final String slug) {
        this.slug = slug;
    }

    public String getBillingEmail() {
        return billingEmail;
    }

    public void setBillingEmail(final String billingEmail) {
        this.billingEmail = billingEmail;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(final String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public OrganizationStatus getStatus() {
        return status;
    }

    public void setStatus(final OrganizationStatus status) {
        this.status = status;
    }

    public boolean isFreeGrantApplied() {
        return freeGrantApplied;
    }

    public void setFreeGrantApplied(final boolean freeGrantApplied) {
        this.freeGrantApplied = freeGrantApplied;
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
