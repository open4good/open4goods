package org.open4goods.nudgerfrontapi.dto.product;

import org.open4goods.model.ai.AiReview;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductReviewDto(
        @Schema(description = "Review language expected to align with the domainLanguage query parameter once localisation is wired.", example = "en")
        String language,

        @Schema(description = "AI-generated review that will align with the requested domainLanguage in future iterations.")
        AiReview review,

        @Schema(description = "Creation timestamp ms", example = "1690972800000")
        long createdMs) {}
