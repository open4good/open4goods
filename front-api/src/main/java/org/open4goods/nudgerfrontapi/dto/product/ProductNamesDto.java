package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Localised textual information.
 */
public record ProductNamesDto(
        @Schema(description = "H1 title")
        String h1Title,
        @Schema(description = "Meta description")
        String metaDescription,
        @Schema(description = "OpenGraph title")
        String ogTitle,
        @Schema(description = "OpenGraph description")
        String ogDescription,
        @Schema(description = "Twitter title")
        String twitterTitle,
        @Schema(description = "Twitter description")
        String twitterDescription
) {}
