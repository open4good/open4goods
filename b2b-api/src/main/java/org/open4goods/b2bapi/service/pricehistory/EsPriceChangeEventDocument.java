package org.open4goods.b2bapi.service.pricehistory;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Read-only Elasticsearch document shape for the {@code o4g-price-change-*} time-series indices
 * described by {@code services/price-history/src/main/resources/elasticsearch/price-change-index-template.json}.
 *
 * <p>Field names mirror the index template mapping exactly (snake_case); this class exists only
 * to deserialize search hits and is never used to write documents (no writer for this index
 * exists yet - see {@code services/price-history} port implementations).
 */
public class EsPriceChangeEventDocument {

    @Id
    @Field(name = "event_id", type = FieldType.Keyword)
    private String eventId;

    @Field(name = "gtin", type = FieldType.Keyword)
    private String gtin;

    @Field(name = "provider_id", type = FieldType.Keyword)
    private String providerId;

    @Field(name = "provider_offer_id", type = FieldType.Keyword)
    private String providerOfferId;

    @Field(name = "condition", type = FieldType.Keyword)
    private String condition;

    @Field(name = "currency", type = FieldType.Keyword)
    private String currency;

    @Field(name = "amount", type = FieldType.Double)
    private Double amount;

    @Field(name = "availability", type = FieldType.Keyword)
    private String availability;

    @Field(name = "event_kind", type = FieldType.Keyword)
    private String eventKind;

    @Field(name = "observed_at", type = FieldType.Date)
    private Instant observedAt;

    @Field(name = "policy_ref", type = FieldType.Keyword)
    private String policyRef;

    @Field(name = "content_hash", type = FieldType.Keyword)
    private String contentHash;

    @Field(name = "@timestamp", type = FieldType.Date_Nanos)
    private Instant timestamp;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    public String getGtin() {
        return gtin;
    }

    public void setGtin(final String gtin) {
        this.gtin = gtin;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(final String providerId) {
        this.providerId = providerId;
    }

    public String getProviderOfferId() {
        return providerOfferId;
    }

    public void setProviderOfferId(final String providerOfferId) {
        this.providerOfferId = providerOfferId;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(final String condition) {
        this.condition = condition;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(final Double amount) {
        this.amount = amount;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(final String availability) {
        this.availability = availability;
    }

    public String getEventKind() {
        return eventKind;
    }

    public void setEventKind(final String eventKind) {
        this.eventKind = eventKind;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(final Instant observedAt) {
        this.observedAt = observedAt;
    }

    public String getPolicyRef() {
        return policyRef;
    }

    public void setPolicyRef(final String policyRef) {
        this.policyRef = policyRef;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(final String contentHash) {
        this.contentHash = contentHash;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final Instant timestamp) {
        this.timestamp = timestamp;
    }
}
