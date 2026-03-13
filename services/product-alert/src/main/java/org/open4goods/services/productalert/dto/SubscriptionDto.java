package org.open4goods.services.productalert.dto;

import java.time.Instant;
import org.open4goods.model.product.ProductCondition;

/**
 * Public representation of a product-alert subscription.
 *
 * @param id deterministic subscription identifier
 * @param email normalized email
 * @param gtin normalized GTIN
 * @param condition watched product condition
 * @param alertPrice optional target price
 * @param alertOnDecrease whether decreases should trigger alerts
 * @param enabled whether the subscription is active
 * @param createdAt creation timestamp
 * @param updatedAt update timestamp
 * @param lastTriggeredAt last trigger timestamp
 * @param lastTriggeredPrice last triggered price
 */
public record SubscriptionDto(
        String id,
        String email,
        String gtin,
        ProductCondition condition,
        Double alertPrice,
        boolean alertOnDecrease,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        Instant lastTriggeredAt,
        Double lastTriggeredPrice)
{
}
