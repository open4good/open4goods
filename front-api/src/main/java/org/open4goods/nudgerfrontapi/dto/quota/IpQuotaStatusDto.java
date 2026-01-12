package org.open4goods.nudgerfrontapi.dto.quota;

import org.open4goods.commons.model.IpQuotaCategory;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing the current IP quota usage for a given category.
 *
 * @param category      category associated with the quota
 * @param limit         maximum number of actions allowed per window
 * @param used          number of actions already performed
 * @param remaining     number of actions still available
 * @param windowSeconds duration of the quota window in seconds
 */
public record IpQuotaStatusDto(
        @Schema(description = "Category associated with the quota.", example = "FEEDBACK_VOTE")
        IpQuotaCategory category,
        @Schema(description = "Maximum number of actions allowed per quota window.", example = "5")
        int limit,
        @Schema(description = "Number of actions already performed within the window.", example = "2")
        int used,
        @Schema(description = "Number of actions remaining within the window.", example = "3")
        int remaining,
        @Schema(description = "Quota window duration in seconds.", example = "86400", nullable = true)
        Long windowSeconds) {
}
