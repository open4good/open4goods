package org.open4goods.icecat.model;

import java.util.Map;

/** Immutable coverage view for the Git-authored Icecat category mappings. */
public record IcecatMappingCoverage(
        int registryVersion,
        String registryHash,
        long categoryCount,
        long mappedCategoryCount,
        long unmappedCategoryCount,
        Map<String, Long> mappedCategoriesByVertical) {
}
