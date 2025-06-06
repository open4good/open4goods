package org.open4goods.services.googlesearch.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing a Google Search Response.
 */
public record GoogleSearchResponse(List<GoogleSearchResult> results) {

    public GoogleSearchResponse() {
        this(new ArrayList<>());
    }

    /** Compatibility accessor preserving former API */
    public List<GoogleSearchResult> getResults() { return results; }
}
