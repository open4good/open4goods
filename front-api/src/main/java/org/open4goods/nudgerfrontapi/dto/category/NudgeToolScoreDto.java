package org.open4goods.nudgerfrontapi.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Score definition exposed by the nudge tool.
 */
public record NudgeToolScoreDto(
        @Schema(description = "Identifier of the score used by the nudge tool.", example = "IMPACT_SCORE")
        String scoreName,
        @Schema(description = "Minimum value required to feature the score.", example = "70")
        Double scoreMinValue,
        @Schema(description = "Material Design icon representing the score.", example = "leaf")
        String mdiIcon,
        @Schema(description = "Localised title of the nudge.")
        String title,
        @Schema(description = "Localised description for the nudge.")
        String description
) {
}
