package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;
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
        @Schema(description = "Singular product name for the requested language", example = "Téléviseur")
        String singular,
        @Schema(description = "Singular designation for the requested language", example = "du téléviseur")
        String singularDesignation,
        @Schema(description = "Generated designation variants for the requested language", example = "[\"du téléviseur\", \"de cette télévision\"]")
        List<String> designation,
        @Schema(description = "Meta description aligned with the requested language")
        String metaDescription,
        @Schema(description = "OpenGraph title for social sharing")
        String ogTitle,
        @Schema(description = "OpenGraph description for social sharing")
        String ogDescription,
        @Schema(description = "Title for product card", example = "Samsung TV")
        String cardTitle,
        @Schema(description = "Short name", example = "Samsung TV")
        String shortName,
        @Schema(description = "Long name", example = "Samsung TV 55 inches 4K")
        String longName,

        @Schema(description = "Offer names aggregated across datasources")
        Set<String> offerNames,
        @Schema(description = "Longest detected offer name", nullable = true)
        String longestOfferName,
        @Schema(description = "Shortest detected offer name", nullable = true)
        String shortestOfferName
) {
}
