package org.open4goods.nudgerfrontapi.dto.blog;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing a blog tag and the number of posts associated with it.
 */
public record BlogTagDto(
        @Schema(description = "Tag name that will be localised using the requested domainLanguage when implemented.", example = "eco")
        String name,
        @Schema(description = "Number of posts having this tag", example = "5", minimum = "0")
        int count
) {}
