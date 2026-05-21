package org.open4goods.services.reviewgeneration.dto;

import java.util.List;
import java.util.Map;

import org.open4goods.model.ai.AiReview;

/**
 * Result returned by an independently executable review-generation stage.
 */
public record ReviewGenerationStepResult(
        long upc,
        String gtin,
        String verticalId,
        String step,
        boolean success,
        String message,
        int sourceCount,
        int totalTokens,
        List<AiReview.AiAttribute> attributes,
        AiReview review,
        ReviewGenerationFailureDetails failureDetails,
        List<String> searchedQueries,
        List<String> acceptedUrls,
        Map<String, String> rejectedUrls) {

    public ReviewGenerationStepResult {
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
        searchedQueries = searchedQueries == null ? List.of() : List.copyOf(searchedQueries);
        acceptedUrls = acceptedUrls == null ? List.of() : List.copyOf(acceptedUrls);
        rejectedUrls = rejectedUrls == null ? Map.of() : Map.copyOf(rejectedUrls);
    }

    public ReviewGenerationStepResult(long upc, String gtin, String verticalId, String step, boolean success,
            String message, int sourceCount, int totalTokens, List<AiReview.AiAttribute> attributes, AiReview review) {
        this(upc, gtin, verticalId, step, success, message, sourceCount, totalTokens, attributes, review, null,
                List.of(), List.of(), Map.of());
    }

    public ReviewGenerationStepResult(long upc, String gtin, String verticalId, String step, boolean success,
            String message, int sourceCount, int totalTokens, List<AiReview.AiAttribute> attributes, AiReview review,
            ReviewGenerationFailureDetails failureDetails) {
        this(upc, gtin, verticalId, step, success, message, sourceCount, totalTokens, attributes, review,
                failureDetails, failureDetails == null ? List.of() : failureDetails.searchedQueries(),
                failureDetails == null ? List.of() : failureDetails.acceptedUrls(),
                failureDetails == null ? Map.of() : failureDetails.rejectedUrls());
    }
}
