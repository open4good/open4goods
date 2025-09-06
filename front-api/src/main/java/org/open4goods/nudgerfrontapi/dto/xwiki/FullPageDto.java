package org.open4goods.nudgerfrontapi.dto.xwiki;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing a rendered XWiki page with metadata.
 */
public record FullPageDto(
        @Schema(description = "HTML meta title", example = "Welcome")
        String metaTitle,

        @Schema(description = "HTML meta description", example = "Description of the page")
        String metaDescription,

        @Schema(description = "Page title", example = "Welcome Page")
        String pageTitle,

        @Schema(description = "Rendered HTML body", example = "<p>Hello</p>")
        String html,

        @Schema(description = "Suggested page width", example = "full")
        String width,

        @Schema(description = "Link to open the page in edit mode", example = "https://wiki/edit")
        String editLink
) {
}
