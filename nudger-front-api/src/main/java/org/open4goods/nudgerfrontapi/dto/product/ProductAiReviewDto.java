package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Map;

import org.open4goods.model.ai.AiReview;

/**
 * DTO exposing AI review information of a product to the frontend.
 */
public record ProductAiReviewDto(
        AiReview review,
        Map<String, Integer> sources,
        boolean enoughData,
        Integer totalTokens,
        Long createdMs) {}
