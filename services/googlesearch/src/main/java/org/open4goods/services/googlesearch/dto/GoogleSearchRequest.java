package org.open4goods.services.googlesearch.dto;

import java.util.Objects;

/**
 * Data Transfer Object representing a Google Search Request.
 */
public class GoogleSearchRequest {
    
    private final String query;
    private final int numResults;

    /**
     * Constructs a new GoogleSearchRequest.
     *
     * @param query the search query
     * @param numResults the number of results to retrieve
     */
    public GoogleSearchRequest(String query, int numResults) {
        this.query = query;
        this.numResults = numResults;
    }

    public String getQuery() {
        return query;
    }

    public int getNumResults() {
        return numResults;
    }

    @Override
    public String toString() {
        return "GoogleSearchRequest{" +
                "query='" + query + '\'' +
                ", numResults=" + numResults +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, numResults);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoogleSearchRequest)) return false;
        GoogleSearchRequest that = (GoogleSearchRequest) o;
        return numResults == that.numResults &&
               Objects.equals(query, that.query);
    }
}
