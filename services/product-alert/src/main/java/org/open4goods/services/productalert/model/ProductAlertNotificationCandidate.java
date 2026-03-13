package org.open4goods.services.productalert.model;

import java.time.Instant;
import org.open4goods.model.product.ProductCondition;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Notification candidate created from a matched subscription and a price-drop
 * event.
 */
@Document(indexName = ProductAlertNotificationCandidate.INDEX_NAME, createIndex = true)
public class ProductAlertNotificationCandidate
{
    /**
     * Elasticsearch index name.
     */
    public static final String INDEX_NAME = "product-alert-notification-candidates";

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String subscriptionId;

    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Long)
    private Long gtin;

    @Field(type = FieldType.Keyword)
    private ProductCondition condition;

    @Field(type = FieldType.Double)
    private Double previousPrice;

    @Field(type = FieldType.Double)
    private Double currentPrice;

    @Field(type = FieldType.Date)
    private Instant eventTimestamp;

    @Field(type = FieldType.Keyword)
    private NotificationCandidateStatus status;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getSubscriptionId()
    {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId)
    {
        this.subscriptionId = subscriptionId;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public Long getGtin()
    {
        return gtin;
    }

    public void setGtin(Long gtin)
    {
        this.gtin = gtin;
    }

    public ProductCondition getCondition()
    {
        return condition;
    }

    public void setCondition(ProductCondition condition)
    {
        this.condition = condition;
    }

    public Double getPreviousPrice()
    {
        return previousPrice;
    }

    public void setPreviousPrice(Double previousPrice)
    {
        this.previousPrice = previousPrice;
    }

    public Double getCurrentPrice()
    {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice)
    {
        this.currentPrice = currentPrice;
    }

    public Instant getEventTimestamp()
    {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp)
    {
        this.eventTimestamp = eventTimestamp;
    }

    public NotificationCandidateStatus getStatus()
    {
        return status;
    }

    public void setStatus(NotificationCandidateStatus status)
    {
        this.status = status;
    }

    public Instant getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt)
    {
        this.createdAt = createdAt;
    }
}
