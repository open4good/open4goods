package org.open4goods.nudgerfrontapi.dto.search;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload returned by the global search endpoint.
 */
public record GlobalSearchResponseDto(
        @Schema(description = "Vertical groups produced by the primary search pass")
        List<GlobalSearchVerticalGroupDto> verticalGroups,
        @Schema(description = "Lexical results that have no vertical assigned")
        List<GlobalSearchResultDto> missingVerticalResults,
        @Schema(description = "Pagination metadata for missing-vertical results")
        GlobalSearchPageMetaDto missingVerticalPage,
        @Schema(description = "Category suggestion if a strict match is found")
        SearchSuggestCategoryDto verticalCta,
        @Schema(description = "Accessories and spare parts without a vertical, set apart from the missing-vertical "
                + "results (at most 8, same shape). Empty when the query itself names an accessory.")
        List<GlobalSearchResultDto> accessoryResults) {
}
