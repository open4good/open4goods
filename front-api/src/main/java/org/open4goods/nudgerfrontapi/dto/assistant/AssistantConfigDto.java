package org.open4goods.nudgerfrontapi.dto.assistant;

import org.open4goods.nudgerfrontapi.dto.category.NudgeToolConfigDto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing an assistant configuration with its identifier.
 */
public record AssistantConfigDto(
        @Schema(description = "Identifier of the assistant configuration.", example = "tv")
        String id,
        @Schema(description = "Nudge tool configuration used by the assistant.")
        NudgeToolConfigDto config
) {
}
