package org.open4goods.icecat.model;

/**
 * A category identity streamed directly from an Icecat reference-catalogue export
 * (CategoriesList.xml or CategoryFeaturesList.xml), independent of any Elasticsearch index.
 *
 * <p>Carries no feature or feature-group data: those are reported only as aggregate counts by
 * {@link IcecatCatalogueInventory}, never promoted to a category-level canonical mapping.
 */
public record IcecatCatalogueCategory(Integer id, String englishName, Integer parentId, Integer score) {
}
