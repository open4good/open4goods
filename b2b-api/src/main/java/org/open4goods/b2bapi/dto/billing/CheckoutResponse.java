package org.open4goods.b2bapi.dto.billing;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response containing the Stripe hosted URL for checkout or customer portal.
 */
@Schema(description = "Response containing the Stripe hosted redirect URL")
public record CheckoutResponse(
        @Schema(description = "Redirect URL to the Stripe hosted page", example = "https://checkout.stripe.com/c/pay/...")
        String url
) {}
