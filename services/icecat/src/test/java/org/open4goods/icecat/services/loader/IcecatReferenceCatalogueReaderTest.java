package org.open4goods.icecat.services.loader;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.icecat.model.IcecatCatalogueCategory;

/**
 * Verifies that {@link IcecatReferenceCatalogueReader} counts and identifies catalogue elements
 * offline, from committed hand-built fixtures, with no network access and no credential.
 */
class IcecatReferenceCatalogueReaderTest {

    @Test
    void readsCategoryIdentitiesFromCategoriesList() throws Exception {
        List<IcecatCatalogueCategory> categories;
        try (InputStream in = resource("/icecat/inventory/CategoriesList-sample.xml")) {
            categories = IcecatReferenceCatalogueReader.readCategories(in, "CategoriesList");
        }

        assertThat(categories).hasSize(3);
        assertThat(categories).extracting(IcecatCatalogueCategory::id).containsExactly(1584, 224, 999);

        IcecatCatalogueCategory tv = categories.get(0);
        assertThat(tv.englishName()).isEqualTo("Televisions");
        assertThat(tv.score()).isEqualTo(10);
        assertThat(tv.parentId()).isNull();

        IcecatCatalogueCategory unreviewed = categories.get(2);
        assertThat(unreviewed.englishName()).isEqualTo("TV-like but unreviewed");
        assertThat(unreviewed.parentId()).isEqualTo(1584);
    }

    @Test
    void readsCategoryIdentitiesFromTheFullAccountCategoryFeaturesListShapeWithoutCapturingNestedFeatures() throws Exception {
        List<IcecatCatalogueCategory> categories;
        try (InputStream in = resource("/icecat/inventory/CategoryFeaturesList-sample.xml")) {
            categories = IcecatReferenceCatalogueReader.readCategories(in, "CategoryFeaturesList");
        }

        assertThat(categories).singleElement().satisfies(category -> {
            assertThat(category.id()).isEqualTo(1584);
            assertThat(category.englishName()).isEqualTo("Televisions");
        });
    }

    @Test
    void countsFeaturesFeatureGroupsAndLanguages() throws Exception {
        try (InputStream in = resource("/icecat/inventory/FeaturesList-sample.xml")) {
            assertThat(IcecatReferenceCatalogueReader.countFeatures(in)).isEqualTo(2);
        }
        try (InputStream in = resource("/icecat/inventory/FeatureGroupsList-sample.xml")) {
            assertThat(IcecatReferenceCatalogueReader.countFeatureGroups(in)).isEqualTo(2);
        }
        try (InputStream in = resource("/icecat/inventory/LanguageList-sample.xml")) {
            assertThat(IcecatReferenceCatalogueReader.countLanguages(in)).isEqualTo(3);
        }
    }

    private InputStream resource(String classpathResource) {
        InputStream in = getClass().getResourceAsStream(classpathResource);
        assertThat(in).as("test fixture " + classpathResource + " must be on the classpath").isNotNull();
        return in;
    }
}
