package org.open4goods.api.dto;

import java.util.List;
import java.util.Map;

/**
 * Category suggestions per datasource, derived from indexed products with a minimum offer count.
 * Intended for agents and tools assembling the {@code matchingCategories} block of a
 * {@code VerticalConfig} YAML without server-side file mutations.
 */
public record CategorySuggestionsDto(
        String vertical,
        int totalProducts,
        Map<String, List<String>> byDatasource
) {}
