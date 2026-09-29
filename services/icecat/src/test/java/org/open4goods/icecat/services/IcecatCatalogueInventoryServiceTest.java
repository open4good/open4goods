package org.open4goods.icecat.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.model.IcecatCatalogueInventory;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;

/**
 * Verifies that {@link IcecatCatalogueInventoryService} builds coverage denominators from a
 * local path, fully offline: no network call, no Open Icecat credential, no beta dependency.
 */
class IcecatCatalogueInventoryServiceTest {

    @Test
    void buildsInventoryFromLocalFixtureFilesWithoutNetworkOrCredentials(@TempDir Path dir) throws Exception {
        File categories = copy(dir, "CategoriesList.xml", "/icecat/inventory/CategoriesList-sample.xml");
        File features = copy(dir, "FeaturesList.xml", "/icecat/inventory/FeaturesList-sample.xml");
        File featureGroups = copy(dir, "FeatureGroupsList.xml", "/icecat/inventory/FeatureGroupsList-sample.xml");
        File languages = copy(dir, "LanguageList.xml", "/icecat/inventory/LanguageList-sample.xml");

        IcecatConfiguration cfg = new IcecatConfiguration();
        cfg.setCategoriesListFileUri(categories.getAbsolutePath());
        cfg.setFeaturesListFileUri(features.getAbsolutePath());
        cfg.setFeatureGroupsFileUri(featureGroups.getAbsolutePath());
        cfg.setLanguageListFileUri(languages.getAbsolutePath());
        // No user/password configured, and the downloader is never invoked for local paths.
        RemoteFileCachingService remoteCaching = Mockito.mock(RemoteFileCachingService.class);
        IcecatFileDownloadService downloader = new IcecatFileDownloadService(remoteCaching, dir.toString(), cfg);

        IcecatCatalogueInventoryService service = new IcecatCatalogueInventoryService(cfg, downloader);
        IcecatCatalogueInventory inventory = service.buildInventory();

        assertThat(inventory.categoryCount()).isEqualTo(3);
        assertThat(inventory.featureCount()).isEqualTo(2);
        assertThat(inventory.featureGroupCount()).isEqualTo(2);
        assertThat(inventory.languageCount()).isEqualTo(3);
        assertThat(inventory.categories()).extracting(c -> c.id()).containsExactly(1584, 224, 999);

        Mockito.verifyNoInteractions(remoteCaching);
    }

    private static File copy(Path dir, String fileName, String classpathResource) throws Exception {
        File dest = new File(dir.toFile(), fileName);
        try (InputStream in = IcecatCatalogueInventoryServiceTest.class.getResourceAsStream(classpathResource)) {
            assertThat(in).as("test fixture " + classpathResource + " must be on the classpath").isNotNull();
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return dest;
    }
}
