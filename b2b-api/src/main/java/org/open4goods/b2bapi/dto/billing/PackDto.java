package org.open4goods.b2bapi.dto.billing;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * DTO representing a prepaid pack of API credits.
 */
@Schema(description = "Prepaid pack of API credits")
public record PackDto(
        @Schema(description = "Internal unique identifier / slug of the pack", example = "growth")
        String id,

        @Schema(description = "Price of the pack in EUR", example = "100.00")
        BigDecimal amountEur,

        @Schema(description = "Number of credits granted by this pack", example = "55000")
        int credits,

        @Schema(description = "Stripe Price ID associated with the pack", example = "price_1PqR...")
        String stripePriceId
) {}
