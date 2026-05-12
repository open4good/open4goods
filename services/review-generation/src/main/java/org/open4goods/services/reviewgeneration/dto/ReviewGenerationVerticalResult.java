package org.open4goods.services.reviewgeneration.dto;

import java.util.List;

/**
 * Synchronous result returned when a review-generation stage is run on a vertical.
 */
public record ReviewGenerationVerticalResult(
        String verticalId,
        String step,
        int requested,
        int processed,
        int succeeded,
        int failed,
        List<ReviewGenerationStepResult> products) {

    public ReviewGenerationVerticalResult {
        products = products == null ? List.of() : List.copyOf(products);
    }
}
