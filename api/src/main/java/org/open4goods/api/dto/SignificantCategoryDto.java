package org.open4goods.api.dto;

/**
 * Category statistically over-represented in a vertical versus unattached products.
 *
 * @param category exact datasourceCategories value
 * @param score Elasticsearch significant_terms score
 * @param fgCount foreground document count in the vertical
 * @param bgCount background document count outside attached verticals
 */
public record SignificantCategoryDto(
        String category,
        double score,
        long fgCount,
        long bgCount
) {}
