package org.open4goods.nudgerfrontapi.dto.category;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lightweight view of a taxonomy node used when rendering children or descendants.
 */
public record GoogleCategorySummaryDto(
        @Schema(description = "Google taxonomy identifier uniquely identifying the node.", example = "1234")
        Integer taxonomyId,
        @Schema(description = "Display name of the category in the requested language.", example = "Téléviseurs")
        String name,
        @Schema(description = "All available localised names keyed by language.")
        Map<String, String> names,
        @Schema(description = "Slug corresponding to the node for the requested language.", example = "televiseurs")
        String slug,
        @Schema(description = "Slug path from the taxonomy root to this node.", example = "electronique/televiseurs")
        String path,
        @Schema(description = "Individual slug segments building the full path.")
        List<String> pathSegments,
        @Schema(description = "Indicates whether the node has any child categories.")
        boolean hasChildren,
        @Schema(description = "Indicates whether the node is a leaf in the taxonomy tree.")
        boolean leaf,
        @Schema(description = "True when a vertical configuration is linked to this node.")
        boolean hasVertical,
        @Schema(description = "True when this node or one of its descendants carries a vertical configuration.")
        boolean hasVerticals,
        @Schema(description = "Vertical configuration summary associated with the node, when available.")
        VerticalConfigDto vertical
) {
}
