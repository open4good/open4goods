package org.open4goods.services.exposeddocs.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contains the raw content of a documentation resource.
 */
public record ExposedDocsContentDto(
        @Schema(description = "Category identifier", example = "docs")
        String categoryId,
        @Schema(description = "Path of the resource", example = "docs/README.md")
        String path,
        @Schema(description = "File extension", example = "md")
        String extension,
        @Schema(description = "Raw file content", example = "# Documentation")
        String content,
        @Schema(description = "Last modified timestamp", format = "date-time")
        Instant lastModified)
{
}
