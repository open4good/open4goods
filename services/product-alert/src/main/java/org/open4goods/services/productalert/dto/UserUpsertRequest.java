package org.open4goods.services.productalert.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload used to upsert a product-alert user.
 *
 * @param email user email address
 */
public record UserUpsertRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email)
{
}
