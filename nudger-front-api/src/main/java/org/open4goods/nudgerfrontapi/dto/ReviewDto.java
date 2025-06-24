package org.open4goods.nudgerfrontapi.dto;

import org.open4goods.model.ai.AiReview;

public record ReviewDto(String language, AiReview review, long createdMs) {}
