package org.open4goods.nudgerfrontapi.dto.search;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Container DTO grouping global search hits by vertical.
 */
public record GlobalSearchVerticalGroupDto(
        @Schema(description = "Identifier of the vertical associated with the group", example = "smartphones")
        String verticalId,
        @Schema(description = "Effective search mode that produced the group", implementation = SearchMode.class)
        SearchMode searchMode,
        @Schema(description = "Ordered results belonging to the vertical")
        List<GlobalSearchResultDto> results) {
}
