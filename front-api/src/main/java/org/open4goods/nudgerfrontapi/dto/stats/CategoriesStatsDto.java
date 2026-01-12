package org.open4goods.nudgerfrontapi.dto.stats;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * DTO exposing aggregated statistics about vertical categories.
 */
public record CategoriesStatsDto(
        @Schema(description = "Total number of enabled vertical configurations (language agnostic but returned alongside domainLanguage).", example = "42")
        int enabledVerticalConfigs,

        @Schema(description = "Number of affiliation partners available for price comparison.", example = "18")
        Integer affiliationPartnersCount,

        @Schema(description = "Total number of GTIN items available in the OpenData exports.", example = "1250000")
        long gtinOpenDataItemsCount,

        @Schema(description = "Total number of ISBN items available in the OpenData exports.", example = "870000")
        long isbnOpenDataItemsCount,

        @Schema(description = "Per-category product counts for recent products with offers, keyed by vertical id.", example = "{\"electronics\": 12000, \"appliances\": 8500}")
        Map<String, Long> productsCountByCategory,

        @Schema(description = "Sum of product counts across enabled categories.", example = "20500")
        long productsCountSum
) { }
