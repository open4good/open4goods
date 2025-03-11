package org.open4goods.services.urlfetching.dto;

import org.open4goods.services.urlfetching.config.FetchStrategy;

/**
 * Data Transfer Object for fetch responses.
 */
public record FetchResponse(String url, int statusCode, String htmlContent, String markdownContent, FetchStrategy fetchStrategy) {

    /**
     * Customized string representation that shows previews of HTML and Markdown content.
     *
     * @return a string representation of the fetch response
     */
    @Override
    public String toString() {
     return fetchStrategy+">" +" : " + url;
    }
}
