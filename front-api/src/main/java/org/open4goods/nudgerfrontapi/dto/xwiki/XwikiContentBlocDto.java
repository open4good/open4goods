package org.open4goods.nudgerfrontapi.dto.xwiki;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing an XWiki content block with its edit link.
 */
public record XwikiContentBlocDto(
        @Schema(description = "Identifier of the XWiki content block")
        String blocId,

        @Schema(description = "Rendered HTML content of the block")
        String htmlContent,

        @Schema(description = "URL to edit the block in XWiki")
        String editLink) {
}
