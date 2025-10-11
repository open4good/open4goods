package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Map;

import org.open4goods.model.ai.AiReview;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing AI review information of a product to the frontend.
 */
public record ProductAiReviewDto(
        @Schema(description = "Language key used to resolve the AI review", example = "fr")
        String language,
        @Schema(description = "AI-generated review content")
        AiReview review,
        @Schema(description = "Source weighting for the review content", nullable = true)
        Map<String, Integer> sources,
        @Schema(description = "Whether enough data was available to generate the review")
        boolean enoughData,
        @Schema(description = "Total tokens consumed by the AI generation", nullable = true)
        Integer totalTokens,
        @Schema(description = "Creation timestamp ms", example = "1690972800000", nullable = true)
        Long createdMs
) {
}
