package org.open4goods.nudgerfrontapi.dto.exposed;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Describes a resource matching a search query.
 */
public record ExposedDocsSearchResultDto(
        @Schema(description = "Category identifier", example = "prompts")
        String categoryId,
        @Schema(description = "Path of the resource", example = "prompts/summary.prompt")
        String path,
        @Schema(description = "File name", example = "summary.prompt")
        String filename,
        @Schema(description = "File extension", example = "prompt")
        String extension,
        @Schema(description = "True when the match was found in content", example = "true")
        boolean contentMatched,
        @Schema(description = "Raw file content when requested", nullable = true)
        String content,
        @Schema(description = "Last modified timestamp", format = "date-time")
        Instant lastModified)
{
}
