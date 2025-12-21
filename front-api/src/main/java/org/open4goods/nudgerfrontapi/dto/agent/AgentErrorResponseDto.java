package org.open4goods.nudgerfrontapi.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Error payload returned when an agent status cannot be resolved.
 *
 * @param message Human readable description of the problem
 */
public record AgentErrorResponseDto(
        @Schema(description = "Message décrivant l'erreur rencontrée.",
                example = "Issue inconnue ou inaccessible.")
        String message) {
}
