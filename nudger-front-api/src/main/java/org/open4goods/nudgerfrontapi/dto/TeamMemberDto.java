package org.open4goods.nudgerfrontapi.dto;

/**
 * Team member information.
 */
import io.swagger.v3.oas.annotations.media.Schema;

public record TeamMemberDto(
        @Schema(description = "Full name", example = "Jane Doe")
        String name,

        @Schema(description = "Role or position", example = "Lead Developer")
        String title) {
}
