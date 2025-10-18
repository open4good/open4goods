package org.open4goods.nudgerfrontapi.dto.category;

import java.util.List;

import org.open4goods.nudgerfrontapi.dto.product.ProductDto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO aggregating the Google taxonomy information required to reproduce the
 * legacy navigation layout.
 */
public record CategoryNavigationDto(
        @Schema(description = "Current taxonomy node selected by the caller.")
        GoogleCategoryDto category,

        @Schema(description = "Breadcrumb entries starting from the catalog root.")
        List<CategoryBreadcrumbItemDto> breadcrumbs,

        @Schema(description = "Immediate children of the selected node filtered by the presence of verticals downstream.")
        List<GoogleCategoryDto> childCategories,

        @Schema(description = "Descendant nodes exposing a vertical configuration but not already present in childCategories.")
        List<GoogleCategoryDto> descendantVerticals,

        @Schema(description = "Popular categories descending from the current node, including indirect children.")
        List<GoogleCategoryDto> popularCategories,

        @Schema(description = "Top five new products for the category ordered by descending impact score. Only the base facet is populated.")
        List<ProductDto> topNewProducts,

        @Schema(description = "Top five occasion products for the category ordered by descending impact score. Only the base facet is populated.")
        List<ProductDto> topOccasionProducts
) {
}

