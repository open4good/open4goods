package org.open4goods.nudgerfrontapi.dto.product;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.open4goods.model.ai.AiReview;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonFilter("inc")
public record ProductReviewDto(
        @Schema(description = "Review language", example = "en")
        String language,

        @Schema(description = "AI-generated review")
        AiReview review,

        @Schema(description = "Creation timestamp ms", example = "1690972800000")
        long createdMs) {}
