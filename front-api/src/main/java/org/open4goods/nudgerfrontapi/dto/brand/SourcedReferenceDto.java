package org.open4goods.nudgerfrontapi.dto.brand;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A sourcing reference backing a brand/company fact.
 *
 * @param url source URL
 * @param label human/provider label (used for attribution, e.g. "Open Supply Hub")
 * @param retrievedAt ISO date the fact was retrieved
 */
@Schema(name = "SourcedReference", description = "Source backing an enriched brand/company fact.")
public record SourcedReferenceDto(
        @Schema(description = "Source URL.", example = "https://opensupplyhub.org/facilities/...")
        String url,
        @Schema(description = "Provider label, used for attribution.", example = "Open Supply Hub", nullable = true)
        String label,
        @Schema(description = "Retrieval date (ISO).", example = "2026-06-01", nullable = true)
        String retrievedAt) {
}
