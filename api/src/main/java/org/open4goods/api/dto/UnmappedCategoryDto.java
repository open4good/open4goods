package org.open4goods.api.dto;

/**
 * Observed vertical category that is not currently present in matchingCategories.
 *
 * @param datasource optional datasource identifier; {@code null} when not derivable from indexed aggregations
 * @param category exact datasourceCategories value
 * @param volume number of products currently carrying the category in this vertical
 */
public record UnmappedCategoryDto(
        String datasource,
        String category,
        long volume
) {}
