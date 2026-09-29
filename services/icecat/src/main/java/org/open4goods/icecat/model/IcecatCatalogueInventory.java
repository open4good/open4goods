package org.open4goods.icecat.model;

import java.util.List;

/**
 * Denominators for Icecat mapping coverage, derived from a streaming read of the Icecat
 * reference-catalogue export (CategoriesList, FeaturesList, FeatureGroupsList, LanguageList).
 *
 * <p>{@code featureGroupCount} counts Icecat's presentation grouping metadata only; it is never
 * a canonical-attribute count and must not be treated as one without an explicit semantic
 * mapping.
 */
public record IcecatCatalogueInventory(
        int featureCount,
        int featureGroupCount,
        int languageCount,
        List<IcecatCatalogueCategory> categories) {

    public int categoryCount() {
        return categories.size();
    }
}
