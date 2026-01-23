package org.open4goods.nudgerfrontapi.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request payload for global product search.
 *
 * @param query free-text query supplied by the caller
 */
public record GlobalSearchRequestDto(
        @Schema(description = "Free-text query used to search across products.",
                example = "Fairphone 4")
        String query) {
}
