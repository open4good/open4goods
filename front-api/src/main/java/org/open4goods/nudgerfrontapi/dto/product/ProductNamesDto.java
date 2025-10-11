package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Localised textual information resolved against the requested domain language.
 */
public record ProductNamesDto(
        @Schema(description = "Canonical product URL for the resolved language", example = "https://example.org/products/123")
        String url,
        @Schema(description = "H1 title for the requested language", example = "Casque Bluetooth haut de gamme")
        String h1Title,
        @Schema(description = "Meta description aligned with the requested language")
        String metaDescription,
        @Schema(description = "OpenGraph title for social sharing")
        String ogTitle,
        @Schema(description = "OpenGraph description for social sharing")
        String ogDescription,
        @Schema(description = "Twitter card title")
        String twitterTitle,
        @Schema(description = "Twitter card description")
        String twitterDescription,
        @Schema(description = "Offer names aggregated across datasources")
        Set<String> offerNames,
        @Schema(description = "Longest detected offer name", nullable = true)
        String longestOfferName,
        @Schema(description = "Shortest detected offer name", nullable = true)
        String shortestOfferName,
        @Schema(description = "All names and descriptions excluding the shortest offer name")
        List<String> namesAndDescriptionsWithoutShortestName
) {
}
