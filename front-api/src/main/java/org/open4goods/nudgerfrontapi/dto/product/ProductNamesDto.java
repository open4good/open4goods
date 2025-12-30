package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Localised textual information resolved against the requested domain language.
 */
public record ProductNamesDto(
        @Schema(description = "H1 title for the requested language", example = "Casque Bluetooth haut de gamme")
        String h1Title,
        @Schema(description = "Pretty name for the requested language", example = "Télévision Samsung 55 \"")
        String prettyName,
        @Schema(description = "Meta description aligned with the requested language")
        String metaDescription,
        @Schema(description = "OpenGraph title for social sharing")
        String ogTitle,
        @Schema(description = "OpenGraph description for social sharing")
        String ogDescription,
        @Schema(description = "Offer names aggregated across datasources")
        Set<String> offerNames,
        @Schema(description = "Longest detected offer name", nullable = true)
        String longestOfferName,
        @Schema(description = "Shortest detected offer name", nullable = true)
        String shortestOfferName
) {
}
