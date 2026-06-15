package org.open4goods.b2bapi.dto.billing;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Mirrored Stripe subscription details")
public record B2bSubscriptionDto(
        @Schema(description = "Subscription UUID", example = "a1a8b9cc-c4d6-4444-ac6b-9c7161b36fa0")
        UUID id,

        @Schema(description = "Stripe subscription ID", example = "sub_123456789")
        String stripeSubscriptionId,

        @Schema(description = "Catalog reference ID of the plan", example = "starter")
        String catalogId,

        @Schema(description = "Status of the subscription", example = "active")
        String status,

        @Schema(description = "End of the current subscription period", example = "2026-07-15T18:00:00Z")
        Instant currentPeriodEnd,

        @Schema(description = "Timestamp when the subscription will be canceled, if set", example = "2026-07-15T18:00:00Z")
        Instant cancelAt,

        @Schema(description = "Timestamp when the subscription mirror was created", example = "2026-06-15T18:22:25Z")
        Instant createdAt
) {}
