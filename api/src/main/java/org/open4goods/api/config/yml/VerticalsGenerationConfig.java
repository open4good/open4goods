package org.open4goods.api.config.yml;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration driving the verticals generation batch.
 */
public record VerticalsGenerationConfig(
        Integer limit,
        String mappingFilePath,
        Set<String> mustExistsFields,
        Double associatedCategoriesEvictionPercent,
        Integer minimumTotalHits) {

    /** Creates config with default values. */
    public VerticalsGenerationConfig() {
        this(null, "/opt/open4goods/config/categories-comappings.json", new HashSet<>(), 0.05, 1);
    }

    /** Canonical constructor applying defaults where necessary. */
    public VerticalsGenerationConfig {
        mappingFilePath = mappingFilePath == null ? "/opt/open4goods/config/categories-comappings.json" : mappingFilePath;
        mustExistsFields = mustExistsFields == null ? new HashSet<>() : mustExistsFields;
        associatedCategoriesEvictionPercent = associatedCategoriesEvictionPercent == null ? 0.05 : associatedCategoriesEvictionPercent;
        minimumTotalHits = minimumTotalHits == null ? 1 : minimumTotalHits;
    }

    // Compatibility accessors -------------------------------------------------
    public Integer getLimit() { return limit; }
    public String getMappingFilePath() { return mappingFilePath; }
    public Set<String> getMustExistsFields() { return mustExistsFields; }
    public Double getAssociatedCategoriesEvictionPercent() { return associatedCategoriesEvictionPercent; }
    public Integer getMinimumTotalHits() { return minimumTotalHits; }
}
