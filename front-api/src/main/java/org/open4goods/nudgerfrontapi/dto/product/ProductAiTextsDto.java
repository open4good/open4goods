package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI generated descriptions facet.
 */
public record ProductAiTextsDto(
        @Schema(description = "AI description aligned with the requested domainLanguage once localisation is available.")
        String description
) {}
