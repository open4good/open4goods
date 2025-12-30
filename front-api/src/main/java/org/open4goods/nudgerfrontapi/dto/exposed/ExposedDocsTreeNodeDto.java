package org.open4goods.nudgerfrontapi.dto.exposed;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a node in the resource tree (folder or file).
 */
public record ExposedDocsTreeNodeDto(
        @Schema(description = "Display name of the node", example = "README.md")
        String name,
        @Schema(description = "Path relative to the category root", example = "docs/README.md")
        String path,
        @Schema(description = "True when the node represents a directory", example = "false")
        boolean directory,
        @Schema(description = "Child nodes, empty for leaf nodes")
        List<ExposedDocsTreeNodeDto> children)
{
}
