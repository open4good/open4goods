package org.open4goods.nudgerfrontapi.dto.category;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The useful texts of the stored AI audit of an ImpactScore, without the raw prompt and response. Generated once
 * per vertical, in French.
 */
public record ImpactScoreAuditDto(
        @Schema(description = "What the score means for a buyer", nullable = true)
        String whatItMeansForUsers,
        @Schema(description = "True when the audit states the score is not an absolute footprint", nullable = true)
        Boolean notAnAbsoluteFootprint,
        @Schema(description = "What a full life-cycle analysis would cover that the score does not", nullable = true)
        String whatIsMissingVsFullLca,
        @Schema(description = "Why a relative score is still useful", nullable = true)
        String whyRelativeIsStillUseful,
        @Schema(description = "Mitigations of the limits of a relative score")
        List<String> mitigations,
        @Schema(description = "Criteria reasoned by the audit, with their justifications")
        List<ImpactScoreAuditCriterionDto> criteria,
        @Schema(description = "Sources cited by the audit")
        List<ImpactScoreAuditSourceDto> sources
) {
}
