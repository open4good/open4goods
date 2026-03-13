package org.open4goods.services.productalert.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.open4goods.model.product.ProductCondition;

/**
 * Request payload used to upsert a subscription.
 *
 * @param email subscriber email
 * @param gtin GTIN to watch
 * @param condition product condition to watch
 * @param alertPrice optional maximum target price
 * @param alertOnDecrease whether price decreases should trigger alerts
 */
public record SubscriptionUpsertRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,
        @NotBlank(message = "gtin is required")
        String gtin,
        @NotNull(message = "condition is required")
        ProductCondition condition,
        Double alertPrice,
        Boolean alertOnDecrease)
{
}
