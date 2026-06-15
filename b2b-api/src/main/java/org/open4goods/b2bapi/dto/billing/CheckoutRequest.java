package org.open4goods.b2bapi.dto.billing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for initiating a Stripe checkout session.
 */
@Schema(description = "Request body for initiating a Stripe checkout session")
public record CheckoutRequest(
        @NotBlank
        @Schema(description = "ID of the catalog item (e.g. 'starter', 'growth', 'scale')", required = true, example = "growth")
        String catalogId
) {}
