package org.open4goods.nudgerfrontapi.dto.feedback;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response returned after casting a vote on a feedback issue.
 */
public record FeedbackVoteResponseDto(
        @Schema(description = "Nombre de votes restants pour l'adresse IP courante.", example = "3")
        int remainingVotes,

        @Schema(description = "Nombre total de votes enregistrés pour l'issue.", example = "15")
        int totalVotes) {
}
