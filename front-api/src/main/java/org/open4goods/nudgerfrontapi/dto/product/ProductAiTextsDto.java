package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI generated descriptions facet resolved using the requested domain language.
 */
public record ProductAiTextsDto(
        @Schema(description = "Language key used to resolve the AI descriptions", example = "fr")
        String language,
        @Schema(description = "AI generated descriptions keyed by prompt identifier")
        Map<String, ProductAiDescriptionDto> descriptions
) {
}
