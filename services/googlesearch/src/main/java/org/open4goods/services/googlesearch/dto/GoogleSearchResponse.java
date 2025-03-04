package org.open4goods.services.googlesearch.dto;

import java.util.List;
import java.util.Objects;

/**
 * Data Transfer Object representing a Google Search Response.
 */
public class GoogleSearchResponse {

    private final List<GoogleSearchResult> results;

    /**
     * Constructs a new GoogleSearchResponse.
     *
     * @param results the list of search results
     */
    public GoogleSearchResponse(List<GoogleSearchResult> results) {
        this.results = results;
    }

    public List<GoogleSearchResult> getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "GoogleSearchResponse{" +
                "results=" + results +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(results);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoogleSearchResponse)) return false;
        GoogleSearchResponse that = (GoogleSearchResponse) o;
        return Objects.equals(results, that.results);
    }
}
