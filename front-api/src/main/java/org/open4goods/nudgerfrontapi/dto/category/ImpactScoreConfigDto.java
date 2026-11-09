package org.open4goods.nudgerfrontapi.dto.category;

import java.util.List;
import java.util.Map;

import org.open4goods.model.vertical.ImpactScoreTexts;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing the impact score configuration with localised explanatory texts.
 */
public record ImpactScoreConfigDto(
        @Schema(description = "Weight applied to each impact score criterion, in the vertical's current methodology.")
        Map<String, Double> criteriasPonderation,
        @Schema(description = "Current ImpactScore methodology version of the vertical.", example = "1")
        int methodologyVersion,
        @Schema(description = "Share of the matrix weight the criteria filled in must reach for a product to be rated; null in methodology version 1.",
                example = "0.6", minimum = "0", maximum = "1", nullable = true)
        Double minRealWeightCoverage,
        @Schema(description = "Position labels, best first; empty in methodology version 1.")
        List<ImpactScorePositionLabelDto> positionLabels,
        @Schema(description = "Fingerprint of the active matrix (weights, sense, bounds).", example = "0123abcd", nullable = true)
        String methodologyRevision,
        @Schema(description = "Useful texts of the stored AI audit; null when the vertical has no audit.", nullable = true)
        ImpactScoreAuditDto audit,
        @Schema(description = "Prompt used to produce the YAML configuration.")
        String yamlPrompt,
        @Schema(description = "AI response that generated the configuration.")
        String aiJsonResponse
) {
}
