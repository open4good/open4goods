package org.open4goods.nudgerfrontapi.dto.feedback;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing the number of votes still available for the current IP address.
 */
public record FeedbackRemainingVotesDto(
        @Schema(description = "Nombre de votes restants pour aujourd'hui.", example = "4")
        int remainingVotes) {
}
