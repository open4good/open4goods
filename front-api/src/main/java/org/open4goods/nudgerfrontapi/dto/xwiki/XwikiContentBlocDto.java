package org.open4goods.nudgerfrontapi.dto.xwiki;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing HTML content extracted from XWiki.
 *
 * @param blocId          identifier of the XWiki page
 * @param htmlContent     HTML representation of the bloc
 * @param editLink        direct edit link for the page
 * @param resolvedLanguage language finally used to render the bloc
 */
public record XwikiContentBlocDto(
        @Schema(description = "Identifier of the XWiki bloc", example = "Main.WebHome")
        String blocId,

        @Schema(description = "Rendered HTML content of the bloc", example = "<p>Translated content</p>")
        String htmlContent,

        @Schema(description = "Direct XWiki edit URL for the bloc", example = "https://wiki.example.org/bin/edit/Main/WebHome")
        String editLink,

        @Schema(description = "Language used to serve the bloc", example = "fr")
        String resolvedLanguage) {
}

