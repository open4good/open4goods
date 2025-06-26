package org.open4goods.nudgerfrontapi.dto.xwiki;

/**
 * DTO representing HTML content extracted from XWiki.
 *
 * @param blocId      identifier of the XWiki page
 * @param htmlContent HTML representation of the bloc
 * @param editLink    direct edit link for the page
 */
public record XwikiContentBlocDto(String blocId, String htmlContent, String editLink) {}

