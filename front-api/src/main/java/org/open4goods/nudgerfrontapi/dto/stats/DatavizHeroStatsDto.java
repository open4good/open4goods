package org.open4goods.nudgerfrontapi.dto.stats;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing headline KPI statistics for the dataviz hero section.
 *
 * <p>
 * This record provides the top-level metrics displayed prominently at the top
 * of a category statistics page. Values are pre-computed on the server so the
 * frontend can render them immediately without additional aggregation calls.
 * </p>
 *
 * @param totalProducts      total number of active products in the vertical
 * @param totalOffers        total number of active commercial offers
 * @param averagePrice       average minimum price across matched products
 * @param medianPrice        median minimum price across matched products
 * @param averageEcoscore    average relative ecoscore value (0-20 scale)
 * @param topBrand           brand with the most products
 * @param topBrandCount      number of products for the top brand
 * @param newProductsPercent percentage of products with condition NEW
 * @param countriesCount     number of distinct manufacturing countries
 * @param dataFreshnessHours hours since the most recently updated product
 */
@Schema(description = "Headline KPI statistics for the dataviz hero section.")
public record DatavizHeroStatsDto(
        @Schema(description = "Total number of active products in the vertical.", example = "1542")
        long totalProducts,

        @Schema(description = "Total number of active commercial offers.", example = "8734")
        long totalOffers,

        @Schema(description = "Average minimum price across matched products.", example = "459.90")
        Double averagePrice,

        @Schema(description = "Median minimum price across matched products.", example = "399.00")
        Double medianPrice,

        @Schema(description = "Average relative ecoscore value (0-20 scale).", example = "12.4", nullable = true)
        Double averageEcoscore,

        @Schema(description = "Brand with the most products in this vertical.", example = "Samsung", nullable = true)
        String topBrand,

        @Schema(description = "Number of products for the top brand.", example = "187", nullable = true)
        Long topBrandCount,

        @Schema(description = "Percentage of products with NEW condition.", example = "82.5")
        Double newProductsPercent,

        @Schema(description = "Number of distinct manufacturing countries.", example = "12")
        int countriesCount,

        @Schema(description = "Hours since the most recently updated product.", example = "2")
        long dataFreshnessHours,

        @Schema(description = "List of dynamically configured KPIs.")
        java.util.List<DatavizHeroKpiValueDto> extraKpis) {
}
