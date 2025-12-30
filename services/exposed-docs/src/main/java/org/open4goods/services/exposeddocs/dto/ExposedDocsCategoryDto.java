package org.open4goods.services.exposeddocs.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Describes a category of exposed resources such as documentation or prompts.
 */
public record ExposedDocsCategoryDto(
        @Schema(description = "Unique identifier of the category", example = "docs")
        String id,
        @Schema(description = "Human-friendly label", example = "Documentation")
        String label,
        @Schema(description = "Endpoint path for the category", example = "/exposed/docs")
        String endpoint,
        @Schema(description = "Classpath root where resources are scanned", example = "docs")
        String classpathRoot,
        @Schema(description = "File extensions included in the category", example = "[\"md\", \"yml\"]")
        List<String> extensions,
        @Schema(description = "Number of resources indexed", example = "42")
        int resourceCount)
{
}
