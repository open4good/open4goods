package org.open4goods.services.reviewgeneration.dto;

import java.util.List;
import java.util.Map;

/**
 * Diagnostics returned when a review-generation step cannot gather enough
 * trustworthy source material.
 */
public record ReviewGenerationFailureDetails(
        int sourceCount,
        int totalTokens,
        List<String> searchedQueries,
        List<String> acceptedUrls,
        Map<String, String> sourceClasses,
        Map<String, String> rejectedUrls) {

    public ReviewGenerationFailureDetails {
        searchedQueries = searchedQueries == null ? List.of() : List.copyOf(searchedQueries);
        acceptedUrls = acceptedUrls == null ? List.of() : List.copyOf(acceptedUrls);
        sourceClasses = sourceClasses == null ? Map.of() : Map.copyOf(sourceClasses);
        rejectedUrls = rejectedUrls == null ? Map.of() : Map.copyOf(rejectedUrls);
    }
}
