package org.open4goods.services.productalert.model;

import java.time.Instant;
import org.open4goods.model.product.ProductCondition;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Product alert subscription stored as one document per email, GTIN and
 * condition.
 */
@Document(indexName = ProductAlertSubscription.INDEX_NAME, createIndex = true)
public class ProductAlertSubscription
{
    /**
     * Elasticsearch index name.
     */
    public static final String INDEX_NAME = "product-alert-subscriptions";

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Long)
    private Long gtin;

    @Field(type = FieldType.Keyword)
    private ProductCondition condition;

    @Field(type = FieldType.Double)
    private Double alertPrice;

    @Field(type = FieldType.Boolean)
    private boolean alertOnDecrease = true;

    @Field(type = FieldType.Boolean)
    private boolean enabled = true;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    @Field(type = FieldType.Date)
    private Instant updatedAt;

    @Field(type = FieldType.Date)
    private Instant lastTriggeredAt;

    @Field(type = FieldType.Double)
    private Double lastTriggeredPrice;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
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

    public Double getAlertPrice()
    {
        return alertPrice;
    }

    public void setAlertPrice(Double alertPrice)
    {
        this.alertPrice = alertPrice;
    }

    public boolean isAlertOnDecrease()
    {
        return alertOnDecrease;
    }

    public void setAlertOnDecrease(boolean alertOnDecrease)
    {
        this.alertOnDecrease = alertOnDecrease;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public Instant getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt)
    {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt()
    {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt)
    {
        this.updatedAt = updatedAt;
    }

    public Instant getLastTriggeredAt()
    {
        return lastTriggeredAt;
    }

    public void setLastTriggeredAt(Instant lastTriggeredAt)
    {
        this.lastTriggeredAt = lastTriggeredAt;
    }

    public Double getLastTriggeredPrice()
    {
        return lastTriggeredPrice;
    }

    public void setLastTriggeredPrice(Double lastTriggeredPrice)
    {
        this.lastTriggeredPrice = lastTriggeredPrice;
    }
}
