package org.open4goods.nudgerfrontapi.dto;

import java.util.Map;

public record ImpactScoreDto(Map<String, Double> scores, double average) {}
