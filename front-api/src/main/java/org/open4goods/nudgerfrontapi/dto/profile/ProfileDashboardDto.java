package org.open4goods.nudgerfrontapi.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Aggregate dashboard payload for the authenticated profile pages.
 *
 * @param displayName authenticated user display name
 * @param trustTier internal trust tier resolved for the user
 * @param identityStatus current identity validation status
 * @param metrics key profile metrics used by dashboard hero and summaries
 */
@Schema(description = "Aggregated profile dashboard data for the authenticated user.")
public record ProfileDashboardDto(
        @Schema(description = "Authenticated user display name.", example = "alice")
        String displayName,
        @Schema(description = "Current trust tier assigned to the user.", example = "trusted")
        String trustTier,
        @Schema(description = "Current internal identity validation status.", example = "pending")
        String identityStatus,
        @Schema(description = "Profile metrics used to render dashboard KPIs.")
        ProfileMetricsDto metrics
) {}
