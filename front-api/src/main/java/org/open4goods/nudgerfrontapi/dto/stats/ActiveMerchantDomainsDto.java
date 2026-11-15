package org.open4goods.nudgerfrontapi.dto.stats;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Dated count of the distinct merchant domains that have at least one active offer (GOU-2037).
 *
 * @param count      number of distinct merchant domains
 * @param computedAt when the count was computed
 * @param franceOnly whether every counted merchant is known to deliver in France
 */
@Schema(description = "Distinct merchant domains (normalised, affiliation networks excluded) having at least one "
        + "offer that is not expired under the offer freshness rule. Computed hourly, out of user requests.")
public record ActiveMerchantDomainsDto(
        @Schema(description = "Number of distinct merchant domains with at least one active offer.", example = "57")
        int count,

        @Schema(description = "When the count was computed.", example = "2026-11-10T08:00:00Z")
        Instant computedAt,

        @Schema(description = "True only when every counted merchant is known to deliver in France; false when the "
                + "delivery country of at least one of them is unknown.", example = "false")
        boolean franceOnly
) { }
