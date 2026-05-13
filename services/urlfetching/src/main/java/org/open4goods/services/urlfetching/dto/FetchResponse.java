package org.open4goods.services.urlfetching.dto;

import java.util.List;
import java.util.Set;

import org.open4goods.services.urlfetching.config.FetchStrategy;

/**
 * Data Transfer Object for fetch responses.
 */
public record FetchResponse(String url, int statusCode, String htmlContent, String markdownContent,
        FetchStrategy fetchStrategy, List<ExtractedMetadataAttribute> metadataAttributes, Set<String> extractedGtins,
        List<ExtractedResource> resources, boolean rejected, String rejectionReason) {

    public FetchResponse
    {
        metadataAttributes = metadataAttributes == null ? List.of() : List.copyOf(metadataAttributes);
        extractedGtins = extractedGtins == null ? Set.of() : Set.copyOf(extractedGtins);
        resources = resources == null ? List.of() : List.copyOf(resources);
    }

    public FetchResponse(String url, int statusCode, String htmlContent, String markdownContent,
            FetchStrategy fetchStrategy) {
        this(url, statusCode, htmlContent, markdownContent, fetchStrategy, List.of(), Set.of(), List.of(), false, null);
    }

    public FetchResponse(String url, int statusCode, String htmlContent, String markdownContent,
            FetchStrategy fetchStrategy, List<ExtractedMetadataAttribute> metadataAttributes, Set<String> extractedGtins,
            boolean rejected, String rejectionReason) {
        this(url, statusCode, htmlContent, markdownContent, fetchStrategy, metadataAttributes, extractedGtins, List.of(),
                rejected, rejectionReason);
    }

    public FetchResponse withRejection(int rejectedStatusCode, String reason) {
        return new FetchResponse(url, rejectedStatusCode, htmlContent, "", fetchStrategy, metadataAttributes,
                extractedGtins, resources, true, reason);
    }

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
