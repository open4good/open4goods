package org.open4goods.nudgerfrontapi.dto.category;

import java.util.Map;

import org.open4goods.model.vertical.ImpactScoreTexts;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing the impact score configuration with localised explanatory texts.
 */
public record ImpactScoreConfigDto(
        @Schema(description = "Weight applied to each impact score criterion.")
        Map<String, Double> criteriasPonderation,
        @Schema(description = "Localised explanatory texts for the impact score.")
        ImpactScoreTexts texts,
        @Schema(description = "Prompt used to produce the YAML configuration.")
        String yamlPrompt,
        @Schema(description = "AI response that generated the configuration.")
        String aiJsonResponse
) {
}
