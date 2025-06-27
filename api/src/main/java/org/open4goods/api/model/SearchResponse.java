package org.open4goods.api.model;

import java.util.List;

/**
 * Search response wrapper used by experimental endpoints.
 */
public record SearchResponse(
        List<SearchResult> results,
        Long recordsTotal,
        Long recordsFiltered) {
}

