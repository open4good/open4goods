package org.open4goods.nudgerfrontapi.dto.share;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lightweight representation of the extracted payload from a shared URL.
 */
public record ShareExtractionDto(
        @Schema(description = "Extracted GTIN when resolvable", example = "7612345678901")
        String gtin,
        @Schema(description = "Query derived from the URL or shared content", example = "fairphone 4 smartphone")
        String query
) {
}
