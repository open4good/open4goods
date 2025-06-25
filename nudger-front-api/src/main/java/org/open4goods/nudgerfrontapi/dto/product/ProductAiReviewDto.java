package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Map;

import org.open4goods.model.ai.AiReview;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI review information for a product.
 */
public record ProductAiReviewDto(
        @Schema(description = "Generated review details")
        AiReview review,

        @Schema(description = "Review sources with estimated token counts", example = "{\"https://example.com\":100}")
        Map<String, Integer> sources,

        @Schema(description = "Enough data available for generation", example = "true")
        boolean enoughData,

        @Schema(description = "Total token count", example = "500")
        Integer totalTokens,

        @Schema(description = "Creation timestamp ms", example = "1690972800000", nullable = true)
        Long createdMs) {}
