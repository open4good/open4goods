package org.open4goods.nudgerfrontapi.dto.xwiki;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing HTML content extracted from XWiki.
 *
 * @param blocId      identifier of the XWiki page
 * @param htmlContent HTML representation of the bloc
 * @param editLink    direct edit link for the page
 */
public record XwikiContentBlocDto(
        @Schema(description = "Identifier of the XWiki bloc", example = "Main.WebHome")
        String blocId,
        @Schema(description = "Rendered HTML content, expected to be localised according to domainLanguage in the future.")
        String htmlContent,
        @Schema(description = "Direct edit link for the XWiki page", nullable = true)
        String editLink) {}

