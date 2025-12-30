package org.open4goods.nudgerfrontapi.dto.exposed;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Provides the list of exposed resource categories.
 */
public record ExposedDocsOverviewDto(
        @Schema(description = "Available resource categories")
        List<ExposedDocsCategoryDto> categories)
{
}
