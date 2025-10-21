package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO describing a single attribute extracted from the AI generated review.
 */
public record AiReviewAttributeDto(
        @Schema(description = "Attribute name extracted from the AI generated content", example = "Battery life")
        String name,

        @Schema(description = "Attribute value summarised by the AI review", example = "Up to 24 hours of usage")
        String value,

        @Schema(description = "Identifier of the cited source supporting this attribute", example = "2")
        Integer number
) {
}
