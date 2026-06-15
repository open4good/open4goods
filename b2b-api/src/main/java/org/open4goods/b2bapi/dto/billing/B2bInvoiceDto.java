package org.open4goods.b2bapi.dto.billing;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Mirrored customer invoice details")
public record B2bInvoiceDto(
        @Schema(description = "Invoice UUID", example = "9ea8b9cc-c4d6-4444-ac6b-9c7161b36fa9")
        UUID id,

        @Schema(description = "Stripe invoice ID", example = "in_123456789")
        String stripeInvoiceId,

        @Schema(description = "Invoice amount in cents", example = "2900")
        int amountCents,

        @Schema(description = "Currency of the invoice", example = "eur")
        String currency,

        @Schema(description = "Invoice status (e.g. paid, open, uncollectible)", example = "paid")
        String status,

        @Schema(description = "Hosted URL of the Stripe invoice", example = "https://invoice.stripe.com/i/acct_...")
        String hostedInvoiceUrl,

        @Schema(description = "Credits granted upon payment of the invoice", example = "12000")
        Long creditsGranted,

        @Schema(description = "Timestamp when the invoice mirror was created", example = "2026-06-15T18:22:25Z")
        Instant createdAt
) {}
