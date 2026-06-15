package org.open4goods.b2bapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Human principal provisioned from an external identity provider.
 */
@Entity
@Table(name = "users")
public class User extends BaseUuidEntity {

    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String email;

    @Column(columnDefinition = "text")
    private String displayName;

    @Column(columnDefinition = "text")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private OidcProvider oidcProvider;

    @Column(nullable = false, columnDefinition = "text")
    private String oidcSubject;

    @Column(name = "is_platform_admin", nullable = false)
    private boolean platformAdmin;

    private Instant lastLoginAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected User() {
    }

    public User(final String email, final OidcProvider oidcProvider, final String oidcSubject) {
        this.email = email;
        this.oidcProvider = oidcProvider;
        this.oidcSubject = oidcSubject;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(final String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public OidcProvider getOidcProvider() {
        return oidcProvider;
    }

    public void setOidcProvider(final OidcProvider oidcProvider) {
        this.oidcProvider = oidcProvider;
    }

    public String getOidcSubject() {
        return oidcSubject;
    }

    public void setOidcSubject(final String oidcSubject) {
        this.oidcSubject = oidcSubject;
    }

    public boolean isPlatformAdmin() {
        return platformAdmin;
    }

    public void setPlatformAdmin(final boolean platformAdmin) {
        this.platformAdmin = platformAdmin;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(final Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
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
