package org.open4goods.services.urlfetching.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the HtmlToMarkdownConverter.
 */
public class HtmlToMarkdownConverterTest {

    @Test
    public void testConversionRemovesLinks() {
        String html = "<html><body><p>Some text <a href=\"http://example.com\">link</a> here.</p></body></html>";
        String markdown = HtmlToMarkdownConverter.convert(html);
        // Markdown should include the link text but not the URL or any <a> tag.
        assertTrue(markdown.contains("link"), "Markdown should contain the link text");
        assertFalse(markdown.contains("http://example.com"), "Markdown should not contain the URL");
        assertFalse(markdown.contains("<a"), "Markdown should not contain any <a> tags");
    }

    @Test
    public void testConversionKeepsOnlyMain() {
        String html = "<html><body>"
                + "<header>Header Content</header>"
                + "<main><h1>Main Content</h1><p>Inside main with <a href=\"http://example.com\">link</a>.</p></main>"
                + "<footer>Footer Content</footer>"
                + "</body></html>";
        String markdown = HtmlToMarkdownConverter.convert(html);
        // Should contain content from <main>
        assertTrue(markdown.contains("Main Content"), "Markdown should contain main section content");
        assertTrue(markdown.contains("Inside main"), "Markdown should contain main section content");
        // Should not contain header or footer content
        assertFalse(markdown.contains("Header Content"), "Markdown should not contain header content");
        assertFalse(markdown.contains("Footer Content"), "Markdown should not contain footer content");
        // Links should be removed within <main>
        assertFalse(markdown.contains("http://example.com"), "Markdown should not contain the URL");
    }

    @Test
    public void testConversionUsesOnlyBodyIfNoMain() {
        String html = "<html>"
                + "<head><title>Test Page</title></head>"
                + "<body><p>Intro text</p><h1>Body Content</h1><p>Paragraph with <a href=\"http://example.com\">link</a>.</p></body>"
                + "</html>";
        String markdown = HtmlToMarkdownConverter.convert(html);
        // Should start from the <h1> element, so "Intro text" is removed.
        assertTrue(markdown.contains("Body Content"), "Markdown should contain body content from <h1> onward");
        assertTrue(markdown.contains("Paragraph with link"), "Markdown should include unwrapped link text");
        assertFalse(markdown.contains("Intro text"), "Markdown should not contain content before <h1>");
        // Ensure that the <head> content is not present.
        assertFalse(markdown.contains("Test Page"), "Markdown should not contain head content");
        // Links should be removed.
        assertFalse(markdown.contains("http://example.com"), "Markdown should not contain the URL");
    }

    @Test
    public void testTrimBeforeFirstHeading() {
        String input = "Preamble text\nSome more text\n*Heading\nContent after heading";
        String trimmed = HtmlToMarkdownConverter.trimBeforeFirstHeading(input);
        String expected = "*Heading\nContent after heading";
        assertEquals(expected, trimmed, "trimBeforeFirstHeading should remove preamble text before the first heading");
    }

    @Test
    public void testContentBeforeFirstH1Removed() {
        String html = "<html><body>"
                + "<div>Content before H1</div>"
                + "<section><h1>Title</h1><p>Content after H1</p></section>"
                + "<footer>Footer Content</footer>"
                + "</body></html>";
        String markdown = HtmlToMarkdownConverter.convert(html);
        // "Content before H1" and "Footer Content" should be removed.
        assertFalse(markdown.contains("Content before H1"), "Markdown should not contain content before <h1>");
        assertTrue(markdown.contains("Title"), "Markdown should contain the <h1> title");
        assertTrue(markdown.contains("Content after H1"), "Markdown should contain content after <h1>");
    }

    @Test
    public void testConversionRemovesImages() {
        String html = "<html><body>"
                + "<h1>Gallery</h1>"
                + "<p>Check out these images:</p>"
                + "<img src=\"http://example.com/image1.jpg\" alt=\"Image 1\" />"
                + "<img src=\"http://example.com/image2.jpg\" alt=\"Image 2\" />"
                + "</body></html>";
        String markdown = HtmlToMarkdownConverter.convert(html);
        // The image markdown should not be present.
        assertFalse(markdown.contains("!["), "Markdown should not contain any image markdown syntax");
        // The text content should remain.
        assertTrue(markdown.contains("Gallery"), "Markdown should contain the <h1> title");
        assertTrue(markdown.contains("Check out these images"), "Markdown should retain paragraph text");
    }
}
