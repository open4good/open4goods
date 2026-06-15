package org.open4goods.b2bapi.dto.billing;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * DTO representing the complete billing catalog including prepaid packs and subscriptions.
 */
@Schema(description = "Complete billing catalog of packs and subscriptions")
public record BillingCatalogDto(
        @Schema(description = "Available prepaid packs")
        List<PackDto> packs,

        @Schema(description = "Available subscription plans")
        List<SubscriptionDto> subscriptions
) {}
