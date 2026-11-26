package org.open4goods.nudgerfrontapi.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lightweight product projection returned by the listing endpoints (category grids, guides, alternatives).
 *
 * <p>It keeps the field names and nesting of {@link ProductDto} so that a card renders from either payload,
 * but only carries what a product card reads: identity, names, card attributes, cover image, ImpactScore,
 * best offers and price trends. The product page keeps the full {@link ProductDto} (GOU-2710).</p>
 *
 * <p>Absent values are omitted from the JSON payload: a missing price, image or score stays missing.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductCardDto(
        @Schema(description = "Product GTIN, it is the unique identifier", example = "7612345678901")
        long gtin,
        @Schema(description = "Product slug", example = "7612345678901-fairphone-4")
        String slug,
        @Schema(description = "Fully qualified slug composed of the vertical home URL and the product slug",
                example = "/telephones-reconditionnes/fairphone-4")
        String fullSlug,
        @Schema(description = "Base metadata used by cards (vertical, best name, cover image)")
        ProductCardBaseDto base,
        @Schema(description = "Brand and model")
        ProductCardIdentityDto identity,
        @Schema(description = "Localised card and display names")
        ProductCardNamesDto names,
        @Schema(description = "Referential attributes and the indexed attributes shown on cards")
        ProductCardAttributesDto attributes,
        @Schema(description = "Cover image and first image")
        ProductCardResourcesDto resources,
        @Schema(description = "ImpactScore status and value")
        ProductCardScoresDto scores,
        @Schema(description = "Offer count, best offers per condition and price trends")
        ProductCardOffersDto offers
) {
}
