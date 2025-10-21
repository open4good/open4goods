package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing a bibliographic source supporting the AI generated review.
 */
public record AiReviewSourceDto(
        @Schema(description = "Ordinal number used inside the generated review to reference the source", example = "1")
        Integer number,

        @Schema(description = "Human readable name of the source", example = "Manufacturer specifications")
        String name,

        @Schema(description = "Short description of the source content", example = "Official datasheet published in 2024")
        String description,

        @Schema(description = "Public URL to access the source", format = "uri", example = "https://example.org/datasheet.pdf")
        String url
) {
}
