package org.open4goods.api.dto;

import java.util.List;

/**
 * Coverage summary for one datasource or inferred datasource bucket in a vertical.
 *
 * @param datasource datasource identifier, or {@code unknown} when not derivable from mappings
 * @param products products currently observed for this datasource bucket
 * @param mappedCategoriesCount number of configured mapped categories for the datasource
 * @param unmappedCategoriesCount number of observed categories missing from the vertical mapping
 * @param sampleUnmapped sample observed categories missing from the vertical mapping
 */
public record DatasourceCoverageDto(
        String datasource,
        long products,
        int mappedCategoriesCount,
        int unmappedCategoriesCount,
        List<String> sampleUnmapped
) {}
