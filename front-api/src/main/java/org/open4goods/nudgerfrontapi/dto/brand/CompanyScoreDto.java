package org.open4goods.nudgerfrontapi.dto.brand;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A company-level ESG / ethics score from a single provider.
 *
 * @param provider provider key (e.g. cdp, bcorp)
 * @param value raw provider value
 * @param rating human-facing rating label
 * @param normalized value normalised to a 0-100 scale (higher is better)
 * @param url source URL
 * @param retrievedAt ISO retrieval date
 */
@Schema(name = "CompanyScore", description = "A company-level score from a single provider.")
public record CompanyScoreDto(
        @Schema(description = "Provider key.", example = "cdp")
        String provider,
        @Schema(description = "Raw provider value.", example = "7.0", nullable = true)
        Double value,
        @Schema(description = "Human-facing rating.", example = "A-", nullable = true)
        String rating,
        @Schema(description = "Normalised 0-100 score (higher is better).", example = "87.5", nullable = true)
        Double normalized,
        @Schema(description = "Source URL.", nullable = true)
        String url,
        @Schema(description = "Retrieval date (ISO).", example = "2026-06-01", nullable = true)
        String retrievedAt) {
}
