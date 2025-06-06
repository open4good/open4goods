package org.open4goods.api.config.yml;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for static resource completion URLs.
 */
public record ResourceCompletionConfig(List<ResourceCompletionUrlTemplate> urlTemplates) {

    /**
     * Default constructor with an empty list.
     */
    public ResourceCompletionConfig() {
        this(new ArrayList<>());
    }

    /**
     * Canonical constructor ensuring non-null list.
     */
    public ResourceCompletionConfig {
        urlTemplates = urlTemplates == null ? new ArrayList<>() : urlTemplates;
    }

    // Compatibility accessor ---------------------------------------------------
    public List<ResourceCompletionUrlTemplate> getUrlTemplates() { return urlTemplates; }
}
