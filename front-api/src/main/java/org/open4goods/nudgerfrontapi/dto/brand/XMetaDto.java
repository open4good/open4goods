package org.open4goods.nudgerfrontapi.dto.brand;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A generic sourced fact about a company (certification, controversy, fact, news).
 *
 * @param key stable key
 * @param type one of certification / controversy / fact / news
 * @param value concise factual text
 * @param url source URL
 * @param retrievedAt ISO retrieval date
 * @param validUntil optional ISO expiry date
 * @param lang optional language tag
 */
@Schema(name = "BrandXMeta", description = "A generic sourced fact attached to a company.")
public record XMetaDto(
        @Schema(description = "Stable key.", example = "certification.bcorp")
        String key,
        @Schema(description = "Fact type.", example = "certification")
        String type,
        @Schema(description = "Concise factual value.", example = "Certified since 2022")
        String value,
        @Schema(description = "Source URL.", nullable = true)
        String url,
        @Schema(description = "Retrieval date (ISO).", example = "2026-06-01", nullable = true)
        String retrievedAt,
        @Schema(description = "Expiry date (ISO).", example = "2027-06-01", nullable = true)
        String validUntil,
        @Schema(description = "Language tag.", example = "en", nullable = true)
        String lang) {
}
