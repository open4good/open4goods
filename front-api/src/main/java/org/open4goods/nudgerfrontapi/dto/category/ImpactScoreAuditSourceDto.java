package org.open4goods.nudgerfrontapi.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A source cited by the stored AI audit of the ImpactScore.
 */
public record ImpactScoreAuditSourceDto(
        @Schema(description = "Identifier of the source in the audit, as cited in its texts", example = "S1", nullable = true)
        String id,
        @Schema(description = "Kind of source (regulation, standard, study...)", nullable = true)
        String type,
        @Schema(description = "Title of the source", nullable = true)
        String title,
        @Schema(description = "Authors or organisation", nullable = true)
        String authorsOrOrg,
        @Schema(description = "Publication year", nullable = true)
        Integer year,
        @Schema(description = "Link to the source", nullable = true)
        String url
) {
}
