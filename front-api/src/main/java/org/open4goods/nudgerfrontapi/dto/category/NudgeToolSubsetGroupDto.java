package org.open4goods.nudgerfrontapi.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Group-level metadata for nudge tool subset screens.
 */
public record NudgeToolSubsetGroupDto(
        @Schema(description = "Identifier of the subset group used to cluster subsets.")
        String id,
        @Schema(description = "Localised title displayed for the group.")
        String title,
        @Schema(description = "Optional description shown under the group title.")
        String description,
        @Schema(description = "Material Design icon decorating the group header.")
        String mdiIcon,
        @Schema(description = "Layout hint for the frontend (e.g. grid, list).")
        String layout,
        @Schema(description = "Custom call-to-action label for the group step.")
        String ctaLabel
) {
}
