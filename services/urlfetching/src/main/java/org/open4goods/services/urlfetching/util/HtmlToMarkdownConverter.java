package org.open4goods.services.urlfetching.util;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;

/**
 * Utility class for converting HTML content to Markdown.
 */
public final class HtmlToMarkdownConverter {

    private static final Logger log = LoggerFactory.getLogger(HtmlToMarkdownConverter.class);

    // Private constructor to prevent instantiation
    private HtmlToMarkdownConverter() { }

    /**
     * Converts HTML content to Markdown.
     *
     * @param html the HTML content
     * @return the Markdown representation of the HTML content
     */
    public static String convert(String html) {
        // Preprocess HTML: extract <main> if present; otherwise, extract <body>.
        String preprocessedHtml = preprocessHtml(html);
        String markdown = FlexmarkHtmlConverter.builder().build().convert(preprocessedHtml);
        return markdown;
    }

    /**
     * Preprocesses HTML content by:
     * <ul>
     *   <li>Unwrapping <code>&lt;a&gt;</code> tags so that links become plain text</li>
     *   <li>If a <code>&lt;main&gt;</code> element is present, using only its content (and logging a warning)</li>
     *   <li>Otherwise, using only the <code>&lt;body&gt;</code> content</li>
     *   <li>Removing all content before the first <code>&lt;h1&gt;</code> element</li>
     *   <li>Removing all <code>&lt;img&gt;</code> elements so that image links are not rendered</li>
     * </ul>
     *
     * @param html the HTML content
     * @return the preprocessed HTML content
     */
    private static String preprocessHtml(String html) {
        Document doc = Jsoup.parse(html);
        Element container;

        // If a <main> section is present, use its content.
        if (!doc.select("main").isEmpty()) {
            log.warn("HTML content contained <main> section. Using <main> section only for conversion.");
            container = doc.selectFirst("main");
        } else {
            // Otherwise, use only the <body> section.
            container = doc.body();
        }

        // Remove <a> tags by unwrapping them.
        container.select("a").unwrap();
        // Remove <img> tags entirely.
        container.select("img").remove();

        // Remove all content before the first <h1> element if present.
        String containerHtml = container.html();
        int h1Pos = containerHtml.indexOf("<h1");
        if (h1Pos != -1) {
            container.html(containerHtml.substring(h1Pos));
        }

        return container.html();
    }

    /**
     * Trims content before the first heading in a markdown string.
     * (Retained for backwards compatibility, but now the Jsoup preprocessing handles the trimming.)
     *
     * @param input the Markdown content
     * @return the trimmed Markdown content
     */
    public static String trimBeforeFirstHeading(String input) {
        if (StringUtils.isEmpty(input)) {
            return input;
        }
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
        return sb.toString().trim();
    }
}
