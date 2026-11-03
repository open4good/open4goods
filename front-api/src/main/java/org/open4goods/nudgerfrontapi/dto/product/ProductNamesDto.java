package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Localised textual information resolved against the requested domain language.
 */
public record ProductNamesDto(
        @Schema(description = "Generic display name for the requested language", example = "Samsung Galaxy S24")
        String displayName,
        @Schema(description = "Short product name for cards, lists and tables", example = "Samsung Galaxy S24")
        String cardName,
        @Schema(description = "Product page H1 title (brand and model), without the manufacturer reference number "
                + "some sources append to the model", example = "ELECTROLUX E62LB200S")
        String pageTitle,
        @Schema(description = "Compact product name used as the base for SEO metadata", example = "Samsung Galaxy S24")
        String seoName,
        @Schema(description = "Backend-computed SEO title (at most 60 characters, never ellipsed): brand, model "
                + "without manufacturer reference, product type, key features and a suffix, degraded when too long. "
                + "Single source of truth for the product page <title>.",
                example = "Hisense WD3I8043BBF2 : lave-linge 8 kg – avis et prix")
        String metaTitle,
        @Schema(description = "Backend-computed meta description (at most 160 characters): impact score out of 20, "
                + "two key features and the price range of the current offers, each omitted when missing. Falls back "
                + "to the stored product meta description when no product name is available.",
                example = "Electrolux E62LB200S, lave-vaisselle : score impact 13/20, 14 couverts, 44 dB, "
                        + "prix de 454,99 à 699,99 €. Comparez les offres sur Nudger.")
        String metaDescription,
        @Schema(description = "OpenGraph title for social sharing")
        String ogTitle,
        @Schema(description = "OpenGraph description for social sharing")
        String ogDescription
) {
}
