package org.open4goods.b2bapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Stripe webhook idempotency and debugging record.
 */
@Entity
@Table(name = "stripe_events")
public class StripeEvent extends BaseUuidEntity {

    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String stripeEventId;

    @Column(nullable = false, columnDefinition = "text")
    private String type;

    @Column(nullable = false)
    private Instant processedAt = Instant.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload = new LinkedHashMap<>();

    protected StripeEvent() {
    }

    public StripeEvent(final String stripeEventId, final String type) {
        this.stripeEventId = stripeEventId;
        this.type = type;
    }

    public String getStripeEventId() {
        return stripeEventId;
    }

    public void setStripeEventId(final String stripeEventId) {
        this.stripeEventId = stripeEventId;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(final Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(final Map<String, Object> payload) {
        this.payload = payload;
    }
}
