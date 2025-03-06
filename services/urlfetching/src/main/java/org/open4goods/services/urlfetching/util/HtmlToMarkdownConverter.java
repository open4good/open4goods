package org.open4goods.services.urlfetching.util;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;

/**
 * Utility class for converting HTML content to Markdown.
 */
public final class HtmlToMarkdownConverter {

    // Private constructor to prevent instantiation
    private HtmlToMarkdownConverter() { }

    /**
     * Converts HTML content to Markdown.
     *
     * @param html the HTML content
     * @return the Markdown representation of the HTML content
     */
    public static String convert(String html) {
        String markdown = FlexmarkHtmlConverter.builder().build().convert(html);
        String trimmed = trimBeforeFirstHeading(markdown);
        return (!trimmed.isEmpty()) ? trimmed : replaceMarkdownLinks(markdown);
    }

    /**
     * Trims content before the first heading.
     *
     * @param input the Markdown content
     * @return the trimmed Markdown content
     */
    public static String trimBeforeFirstHeading(String input) {
        String[] lines = input.split("\n");
        StringBuilder sb = new StringBuilder();
        boolean foundHeading = false;
        for (String line : lines) {
            if (!foundHeading && line.startsWith("*") && line.length() > 1 && line.charAt(1) != ' ') {
                foundHeading = true;
            }
            if (foundHeading) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Replaces Markdown links with just their text.
     *
     * @param input the Markdown content
     * @return the Markdown content without hyperlinks
     */
    public static String replaceMarkdownLinks(String input) {
        String regex = "\\[(.*?)\\]\\((https?://[^\\s)]+)(?:\\s+\"[^\"]*\")?\\)";
        return input.replaceAll(regex, "$1");
    }
}
