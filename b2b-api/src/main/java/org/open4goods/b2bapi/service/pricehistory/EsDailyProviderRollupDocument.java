package org.open4goods.b2bapi.service.pricehistory;

import java.time.Instant;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Read-only Elasticsearch document shape for the {@code o4g-daily-provider-rollup-*} time-series
 * indices described by
 * {@code services/price-history/src/main/resources/elasticsearch/daily-provider-rollup-index-template.json}.
 *
 * <p>Field names mirror the index template mapping exactly (snake_case); this class exists only
 * to deserialize search hits and is never used to write documents (no writer for this index
 * exists yet - see {@code services/price-history} port implementations).
 */
public class EsDailyProviderRollupDocument {

    @Field(name = "@timestamp", type = FieldType.Date_Nanos)
    private Instant timestamp;

    @Field(name = "gtin", type = FieldType.Keyword)
    private String gtin;

    @Field(name = "provider_id", type = FieldType.Keyword)
    private String providerId;

    @Field(name = "condition", type = FieldType.Keyword)
    private String condition;

    @Field(name = "currency", type = FieldType.Keyword)
    private String currency;

    @Field(name = "minimum_amount", type = FieldType.Double)
    private Double minimumAmount;

    @Field(name = "maximum_amount", type = FieldType.Double)
    private Double maximumAmount;

    @Field(name = "close_amount", type = FieldType.Double)
    private Double closeAmount;

    @Field(name = "observed_offer_count", type = FieldType.Long)
    private Long observedOfferCount;

    @Field(name = "change_count", type = FieldType.Long)
    private Long changeCount;

    @Field(name = "first_observed_at", type = FieldType.Date)
    private Instant firstObservedAt;

    @Field(name = "last_observed_at", type = FieldType.Date)
    private Instant lastObservedAt;

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final Instant timestamp) {
        this.timestamp = timestamp;
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

    public Double getMinimumAmount() {
        return minimumAmount;
    }

    public void setMinimumAmount(final Double minimumAmount) {
        this.minimumAmount = minimumAmount;
    }

    public Double getMaximumAmount() {
        return maximumAmount;
    }

    public void setMaximumAmount(final Double maximumAmount) {
        this.maximumAmount = maximumAmount;
    }

    public Double getCloseAmount() {
        return closeAmount;
    }

    public void setCloseAmount(final Double closeAmount) {
        this.closeAmount = closeAmount;
    }

    public Long getObservedOfferCount() {
        return observedOfferCount;
    }

    public void setObservedOfferCount(final Long observedOfferCount) {
        this.observedOfferCount = observedOfferCount;
    }

    public Long getChangeCount() {
        return changeCount;
    }

    public void setChangeCount(final Long changeCount) {
        this.changeCount = changeCount;
    }

    public Instant getFirstObservedAt() {
        return firstObservedAt;
    }

    public void setFirstObservedAt(final Instant firstObservedAt) {
        this.firstObservedAt = firstObservedAt;
    }

    public Instant getLastObservedAt() {
        return lastObservedAt;
    }

    public void setLastObservedAt(final Instant lastObservedAt) {
        this.lastObservedAt = lastObservedAt;
    }
}
