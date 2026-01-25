package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Container DTO for the AI review resolved for a product and its associated metadata.
 */
public record ProductAiReviewDto(
        @Schema(description = "Language key that matched the AI review inside the Localisable structure", example = "fr-FR")
        String language,

        @Schema(description = "AI generated review content aligned with the requested domain language")
        AiReviewDto review,

        @Schema(description = "Source weighting for the review content", type = "object")
        Map<String, Integer> sources,

        @Schema(description = "Whether enough data was available to trigger the AI review generation")
        Boolean enoughData,

        @Schema(description = "Total tokens consumed by the generation pipeline", example = "2300")
        Integer totalTokens,

        @Schema(description = "Creation timestamp in epoch milliseconds", example = "1690972800000")
        Long createdMs,

        @Schema(description = "Reason why generation failed if enoughData is false or review is missing")
        String failureReason
) {
}
