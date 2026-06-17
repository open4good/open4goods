package org.open4goods.b2bapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Barcode asset entity representing temporarily cached barcode images.
 */
@Entity
@Table(name = "barcode_assets")
public class BarcodeAsset extends BaseUuidEntity {

    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String token;

    @Column(nullable = false)
    private byte[] content;

    @Column(nullable = false, columnDefinition = "text")
    private String contentType;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected BarcodeAsset() {
    }

    public BarcodeAsset(final String token, final byte[] content, final String contentType, final Instant expiresAt) {
        this.token = token;
        this.content = content;
        this.contentType = contentType;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(final byte[] content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(final Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }
}
