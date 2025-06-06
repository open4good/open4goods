package org.open4goods.services.googlesearch.dto;

import java.util.Objects;

/**
 * Data Transfer Object representing an individual search result.
 */
public record GoogleSearchResult(String title, String link) {

    public GoogleSearchResult() {
        this(null, null);
    }
}
