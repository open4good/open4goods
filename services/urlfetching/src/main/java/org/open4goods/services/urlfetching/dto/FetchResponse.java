package org.open4goods.services.urlfetching.dto;

/**
 * Data Transfer Object for fetch responses.
 */
public record FetchResponse(int statusCode, String htmlContent, String markdownContent) {

    /**
     * Customized string representation that shows previews of HTML and Markdown content.
     *
     * @return a string representation of the fetch response
     */
    @Override
    public String toString() {
        String htmlPreview = htmlContent != null
                ? htmlContent.substring(0, Math.min(100, htmlContent.length())) + "..."
                : "null";
        String markdownPreview = markdownContent != null
                ? markdownContent.substring(0, Math.min(100, markdownContent.length())) + "..."
                : "null";
        return "FetchResponse{" +
                "statusCode=" + statusCode +
                ", htmlContent='" + htmlPreview + '\'' +
                ", markdownContent='" + markdownPreview + '\'' +
                '}';
    }
}
