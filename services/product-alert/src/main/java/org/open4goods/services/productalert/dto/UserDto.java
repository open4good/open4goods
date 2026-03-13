package org.open4goods.services.productalert.dto;

import java.time.Instant;
import org.open4goods.services.productalert.model.ProductAlertUserStatus;

/**
 * Public representation of a product-alert user.
 *
 * @param email normalized email
 * @param status user status
 * @param createdAt creation timestamp
 * @param updatedAt update timestamp
 */
public record UserDto(
        String email,
        ProductAlertUserStatus status,
        Instant createdAt,
        Instant updatedAt)
{
}
