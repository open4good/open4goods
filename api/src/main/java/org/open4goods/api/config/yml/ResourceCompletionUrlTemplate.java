package org.open4goods.api.config.yml;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.model.resource.ResourceTag;

/**
 * Template definition for building completion resource URLs.
 */
public record ResourceCompletionUrlTemplate(
        String url,
        String datasourceName,
        String language,
        List<ResourceTag> hardTags) {

    /**
     * Creates an empty template.
     */
    public ResourceCompletionUrlTemplate() {
        this(null, null, null, new ArrayList<>());
    }

    /**
     * Canonical constructor ensuring non-null list of tags.
     */
    public ResourceCompletionUrlTemplate {
        hardTags = hardTags == null ? new ArrayList<>() : hardTags;
    }

    // Compatibility accessors -------------------------------------------------
    public String getUrl() { return url; }
    public String getDatasourceName() { return datasourceName; }
    public String getLanguage() { return language; }
    public List<ResourceTag> getHardTags() { return hardTags; }
}
