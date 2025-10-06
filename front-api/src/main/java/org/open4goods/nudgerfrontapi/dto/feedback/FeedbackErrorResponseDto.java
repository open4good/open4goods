package org.open4goods.nudgerfrontapi.dto.feedback;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Generic error payload returned by feedback endpoints when an operation fails.
 */
public record FeedbackErrorResponseDto(
        @Schema(description = "Message décrivant la raison de l'erreur.", example = "Le captcha est invalide.")
        String message) {
}
