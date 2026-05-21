package org.open4goods.services.reviewgeneration.dto;

import java.util.List;

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
        ReviewGenerationFailureDetails failureDetails) {

    public ReviewGenerationStepResult {
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
    }

    public ReviewGenerationStepResult(long upc, String gtin, String verticalId, String step, boolean success,
            String message, int sourceCount, int totalTokens, List<AiReview.AiAttribute> attributes, AiReview review) {
        this(upc, gtin, verticalId, step, success, message, sourceCount, totalTokens, attributes, review, null);
    }
}
