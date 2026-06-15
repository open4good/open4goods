package org.open4goods.b2bapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Append-only audit event for platform-admin actions.
 */
@Entity
@Table(name = "admin_audit_events")
public class AdminAuditEvent extends BaseUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private User actorUser;

    @Column(nullable = false, columnDefinition = "text")
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_organization_id")
    private Organization targetOrganization;

    @Column(columnDefinition = "text")
    private String targetRef;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> detail = new LinkedHashMap<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected AdminAuditEvent() {
    }

    public AdminAuditEvent(final User actorUser, final String action) {
        this.actorUser = actorUser;
        this.action = action;
    }

    public User getActorUser() {
        return actorUser;
    }

    public void setActorUser(final User actorUser) {
        this.actorUser = actorUser;
    }

    public String getAction() {
        return action;
    }

    public void setAction(final String action) {
        this.action = action;
    }

    public Organization getTargetOrganization() {
        return targetOrganization;
    }

    public void setTargetOrganization(final Organization targetOrganization) {
        this.targetOrganization = targetOrganization;
    }

    public String getTargetRef() {
        return targetRef;
    }

    public void setTargetRef(final String targetRef) {
        this.targetRef = targetRef;
    }

    public Map<String, Object> getDetail() {
        return detail;
    }

    public void setDetail(final Map<String, Object> detail) {
        this.detail = detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }
}
