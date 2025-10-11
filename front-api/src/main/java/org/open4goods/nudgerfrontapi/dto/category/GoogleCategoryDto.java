package org.open4goods.nudgerfrontapi.dto.category;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Detailed representation of a Google taxonomy node enriched with vertical data and navigation helpers.
 */
public record GoogleCategoryDto(
        @Schema(description = "Google taxonomy identifier of the category.", example = "1234")
        Integer taxonomyId,
        @Schema(description = "Display name of the category in the requested language.", example = "Téléviseurs")
        String name,
        @Schema(description = "All available localised names keyed by language.")
        Map<String, String> names,
        @Schema(description = "Slug corresponding to the category for the requested language.", example = "televiseurs")
        String slug,
        @Schema(description = "Slug path from the taxonomy root to this category.", example = "electronique/televiseurs")
        String path,
        @Schema(description = "Individual slug segments that compose the full path.")
        List<String> pathSegments,
        @Schema(description = "Indicates whether the category has children filtered to nodes exposing verticals.")
        boolean hasChildren,
        @Schema(description = "True when the category does not have any children in the taxonomy tree.")
        boolean leaf,
        @Schema(description = "True when a vertical configuration is directly associated with the category.")
        boolean hasVertical,
        @Schema(description = "True when the category or one of its descendants exposes a vertical configuration.")
        boolean hasVerticals,
        @Schema(description = "Summary of the associated vertical configuration, when available.")
        VerticalConfigDto vertical,
        @Schema(description = "Breadcrumb elements describing the navigation path to this category.")
        List<GoogleCategoryBreadcrumbDto> breadcrumbs,
        @Schema(description = "Immediate child categories filtered to nodes exposing verticals.")
        List<GoogleCategorySummaryDto> children,
        @Schema(description = "Descendant categories carrying a vertical configuration but not listed as direct children.")
        List<GoogleCategorySummaryDto> descendantVerticals
) {
}
