package org.open4goods.services.googlesearch.dto;

import java.util.Objects;

/**
 * Data Transfer Object representing an individual search result.
 */
public record GoogleSearchResult(String title, String link) {

    public GoogleSearchResult() {
        this(null, null);
    }

    /** Compatibility accessor preserving former API */
    public String getTitle() { return title; }

    /** Compatibility accessor preserving former API */
    public String getLink() { return link; }
}
