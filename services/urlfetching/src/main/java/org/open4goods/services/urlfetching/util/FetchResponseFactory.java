package org.open4goods.services.urlfetching.util;

import org.open4goods.services.urlfetching.config.FetchStrategy;
import org.open4goods.services.urlfetching.dto.FetchResponse;

/**
 * Builds fetch responses after extracting rich metadata before markdown conversion.
 */
public final class FetchResponseFactory {

    private FetchResponseFactory() {
    }

    public static FetchResponse fromHtml(String url, int statusCode, String htmlContent, FetchStrategy fetchStrategy) {
        StructuredMetadataExtractor.ExtractionResult metadata = StructuredMetadataExtractor.extract(htmlContent);
        var resources = OfficialResourceExtractor.extract(url, htmlContent, metadata.attributes());
        String markdownContent = HtmlToMarkdownConverter.convert(htmlContent);
        return new FetchResponse(url, statusCode, htmlContent, markdownContent, fetchStrategy, metadata.attributes(),
                metadata.gtins(), resources, false, null);
    }
}
