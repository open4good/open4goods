package org.open4goods.icecat.services.loader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.jaxb.Category;
import org.open4goods.icecat.jaxb.Name;
import org.open4goods.icecat.services.IcecatFileDownloadService;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.verticals.VerticalsConfigService;

/**
 * Verifies that {@link CategoryLoader} unmarshals a real CategoriesList.xml fixture into the
 * JAXB-generated {@link Category} contract with field-for-field parity, not just non-null results.
 */
public class CategoryLoaderTest {

    @Test
    public void loadCategoriesParsesRealValuesFromFixture(@TempDir Path cacheDir) throws Exception {
        String categoriesUri = "test://CategoriesList.xml";
        seedCache(cacheDir, categoriesUri, "/icecat/CategoriesList-sample.xml");

        IcecatConfiguration cfg = new IcecatConfiguration();
        cfg.setCategoriesListFileUri(categoriesUri);
        IcecatFileDownloadService downloader = new IcecatFileDownloadService(
                Mockito.mock(RemoteFileCachingService.class), cacheDir.toString(), cfg);
        VerticalsConfigService verticalsConfigService = Mockito.mock(VerticalsConfigService.class);
        FeatureLoader featureLoader = Mockito.mock(FeatureLoader.class);
        CategoryLoader loader = new CategoryLoader(cfg, downloader, verticalsConfigService, featureLoader);

        assertDoesNotThrow(loader::loadCategories);

        Category category = loader.getCategoriesById().get(100);
        assertNotNull(category, "category 100 should have been parsed from the fixture");
        assertEquals(BigInteger.valueOf(10), category.getScore());

        Name englishName = category.getName().stream()
                .filter(n -> IcecatBulkModelSupport.intValue(n.getLangid(), -1) == 1)
                .findFirst()
                .orElseThrow();
        assertEquals("Washing Machines", IcecatBulkModelSupport.effectiveName(englishName));

        Name frenchName = category.getName().stream()
                .filter(n -> IcecatBulkModelSupport.intValue(n.getLangid(), -1) == 3)
                .findFirst()
                .orElseThrow();
        assertEquals("Lave-linge", IcecatBulkModelSupport.effectiveName(frenchName));
    }

    private static void seedCache(Path cacheDir, String uri, String classpathResource) throws Exception {
        File cachedFile = new File(cacheDir.toFile(), IdHelper.getHashedName(uri));
        try (InputStream in = CategoryLoaderTest.class.getResourceAsStream(classpathResource)) {
            assertNotNull(in, "test fixture " + classpathResource + " must be on the classpath");
            Files.copy(in, cachedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
