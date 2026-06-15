package org.open4goods.b2bapi.dto.billing;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * DTO representing a recurring subscription plan.
 */
@Schema(description = "Recurring subscription plan")
public record SubscriptionDto(
        @Schema(description = "Internal unique identifier / slug of the subscription", example = "growth")
        String id,

        @Schema(description = "Monthly price of the subscription in EUR", example = "100.00")
        BigDecimal amountEur,

        @Schema(description = "Credits granted each month under this plan", example = "66000")
        int monthlyCredits,

        @Schema(description = "Maximum number of months that credits can roll over (0 for no rollover)", example = "3")
        int rolloverCapMonths,

        @Schema(description = "Stripe Price ID associated with the subscription", example = "price_1PqS...")
        String stripePriceId
) {}
