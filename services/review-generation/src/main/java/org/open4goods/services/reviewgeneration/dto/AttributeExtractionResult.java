package org.open4goods.services.reviewgeneration.dto;

import java.util.List;

import org.open4goods.model.ai.AiReview;

/**
 * Carries the structured attributes extracted by the first LLM phase
 * (review-generation-attributes prompt) before text generation.
 */
public record AttributeExtractionResult(List<AiReview.AiAttribute> attributes) {

    public AttributeExtractionResult {
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
    }
}
