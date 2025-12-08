package org.open4goods.nudgerfrontapi.dto.category;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Frontend representation of the nudge tool configuration.
 */
public record NudgeToolConfigDto(
        @Schema(description = "List of scores highlighted by the nudge tool.")
        List<NudgeToolScoreDto> scores,
        @Schema(description = "Subsets reused by the nudge tool.")
        List<VerticalSubsetDto> subsets,
        @Schema(description = "Group metadata to organise subset screens.")
        List<NudgeToolSubsetGroupDto> subsetGroups
) {
}
