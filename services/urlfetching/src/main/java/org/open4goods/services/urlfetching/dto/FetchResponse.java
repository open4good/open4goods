package org.open4goods.services.urlfetching.dto;

/**
 * Data Transfer Object for fetch responses.
 */
public class FetchResponse {

    private final int statusCode;
    private final String htmlContent;
    private final String markdownContent;

    /**
     * Constructs a new FetchResponse.
     *
     * @param statusCode      the HTTP status code of the response
     * @param htmlContent     the raw HTML content fetched
     * @param markdownContent the markdown representation of the HTML content
     */
    public FetchResponse(int statusCode, String htmlContent, String markdownContent) {
        this.statusCode = statusCode;
        this.htmlContent = htmlContent;
        this.markdownContent = markdownContent;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public String getMarkdownContent() {
        return markdownContent;
    }

    @Override
    public String toString() {
        return "FetchResponse{" +
                "statusCode=" + statusCode +
                ", htmlContent='" + (htmlContent != null
                    ? htmlContent.substring(0, Math.min(100, htmlContent.length())) + "..."
                    : "null") + '\'' +
                ", markdownContent='" + (markdownContent != null
                    ? markdownContent.substring(0, Math.min(100, markdownContent.length())) + "..."
                    : "null") + '\'' +
                '}';
    }
}
