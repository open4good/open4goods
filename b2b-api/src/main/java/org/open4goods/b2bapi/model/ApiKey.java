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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Hashed Product Data API key metadata.
 */
@Entity
@Table(name = "api_keys")
public class ApiKey extends BaseUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String keyPrefix;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, unique = true, length = 64, columnDefinition = "char(64)")
    private String keyHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private ApiKeyStatus status = ApiKeyStatus.ACTIVE;

    private Instant lastUsedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rotated_from")
    private ApiKey rotatedFrom;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant revokedAt;

    protected ApiKey() {
    }

    public ApiKey(final Organization organization, final User createdBy, final String name,
            final String keyPrefix, final String keyHash) {
        this.organization = organization;
        this.createdBy = createdBy;
        this.name = name;
        this.keyPrefix = keyPrefix;
        this.keyHash = keyHash;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(final Organization organization) {
        this.organization = organization;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final User createdBy) {
        this.createdBy = createdBy;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(final String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(final String keyHash) {
        this.keyHash = keyHash;
    }

    public ApiKeyStatus getStatus() {
        return status;
    }

    public void setStatus(final ApiKeyStatus status) {
        this.status = status;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(final Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public ApiKey getRotatedFrom() {
        return rotatedFrom;
    }

    public void setRotatedFrom(final ApiKey rotatedFrom) {
        this.rotatedFrom = rotatedFrom;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(final Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
