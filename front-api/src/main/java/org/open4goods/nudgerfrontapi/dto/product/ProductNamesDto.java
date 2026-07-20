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
        @Schema(description = "Product page H1 title", example = "Samsung Galaxy S24")
        String pageTitle,
        @Schema(description = "Compact product name used as the base for SEO metadata", example = "Samsung Galaxy S24")
        String seoName,
        @Schema(description = "Backend-computed SEO meta title aligned with the requested language",
                example = "Samsung Galaxy S24 - score impact 14/20 | Nudger")
        String metaTitle,
        @Schema(description = "Meta description aligned with the requested language")
        String metaDescription,
        @Schema(description = "OpenGraph title for social sharing")
        String ogTitle,
        @Schema(description = "OpenGraph description for social sharing")
        String ogDescription
) {
}
