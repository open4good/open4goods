package org.open4goods.services.googlesearch.dto;

/**
 * Data Transfer Object representing a Google Search Request.
 */
public record GoogleSearchRequest(
        String query,
        int numResults,
        String lr,
        String cr,
        String safe,
        String sort,
        String gl,
        String hl) {

    private static final int DEFAULT_SEARCH_RESULTS = 10;

    public GoogleSearchRequest(String query, String lr, String cr) {
        this(query, DEFAULT_SEARCH_RESULTS, lr, cr, "off", null, null, null);
    }

    public GoogleSearchRequest {
        safe = (safe == null || safe.isBlank()) ? "off" : safe;
    }
}
