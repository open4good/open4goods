package org.open4goods.nudgerfrontapi.dto.feedback;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO indicating whether the current IP is allowed to cast another vote today.
 */
public record FeedbackVoteEligibilityDto(
        @Schema(description = "Vrai si l'utilisateur peut encore voter aujourd'hui.", example = "true")
        boolean canVote) {
}
