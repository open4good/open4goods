package org.open4goods.nudgerfrontapi.dto.feedback;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response returned after a feedback submission attempt.
 */
public record FeedbackSubmissionResponseDto(
        @Schema(description = "Indique si le ticket GitHub a été créé avec succès.", example = "true")
        boolean success,

        @Schema(description = "Numéro GitHub de l'issue créée.", example = "128", nullable = true)
        Integer issueNumber,

        @Schema(description = "URL publique de l'issue GitHub.",
                example = "https://github.com/open4good/open4goods/issues/128", nullable = true, format = "uri")
        String issueUrl,

        @Schema(description = "Message informatif pour l'utilisateur.", example = "Merci pour votre retour !", nullable = true)
        String message) {
}
