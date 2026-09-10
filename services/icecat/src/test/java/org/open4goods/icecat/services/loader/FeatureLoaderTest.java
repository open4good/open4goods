package org.open4goods.icecat.services.loader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.open4goods.brand.service.BrandService;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.jaxb.Feature;
import org.open4goods.icecat.jaxb.Name;
import org.open4goods.icecat.services.IcecatFileDownloadService;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;

/**
 * Verifies that {@link FeatureLoader} unmarshals a real FeaturesList.xml fixture into the
 * JAXB-generated {@link Feature} contract with field-for-field parity, not just non-null results.
 */
public class FeatureLoaderTest {

    @Test
    public void loadFeaturesParsesRealValuesFromFixture(@TempDir Path cacheDir) throws Exception {
        String featuresUri = "test://FeaturesList.xml";
        seedCache(cacheDir, featuresUri, "/icecat/FeaturesList-sample.xml");

        IcecatConfiguration cfg = new IcecatConfiguration();
        cfg.setFeaturesListFileUri(featuresUri);
        IcecatFileDownloadService downloader = new IcecatFileDownloadService(
                Mockito.mock(RemoteFileCachingService.class), cacheDir.toString(), cfg);
        BrandService brandService = Mockito.mock(BrandService.class);
        FeatureLoader loader = new FeatureLoader(cfg, downloader, brandService);

        assertDoesNotThrow(loader::loadFeatures);

        Feature feature = loader.getFeaturesById().get(42);
        assertNotNull(feature, "feature 42 should have been parsed from the fixture");
        assertEquals("numerical", feature.getType());
        assertEquals(BigInteger.valueOf(1), feature.getMandatory());
        assertTrue(feature.isSetSearchable() && feature.isSearchable());
        assertTrue(feature.isSetDefaultDisplayUnit() && feature.isDefaultDisplayUnit());
        assertTrue(feature.isSetClazz() && !feature.isClazz());
        assertEquals(BigInteger.valueOf(3), feature.getNo());
        assertEquals(BigInteger.valueOf(7), feature.getCategoryFeatureGroupID());

        Name englishName = feature.getNames().getName().stream()
                .filter(n -> IcecatBulkModelSupport.intValue(n.getLangid(), -1) == 1)
                .findFirst()
                .orElseThrow();
        assertEquals("Screen size", IcecatBulkModelSupport.effectiveName(englishName));

        Name frenchName = feature.getNames().getName().stream()
                .filter(n -> IcecatBulkModelSupport.intValue(n.getLangid(), -1) == 3)
                .findFirst()
                .orElseThrow();
        assertEquals("Taille d'ecran", IcecatBulkModelSupport.effectiveName(frenchName));
    }

    private static void seedCache(Path cacheDir, String uri, String classpathResource) throws Exception {
        File cachedFile = new File(cacheDir.toFile(), IdHelper.getHashedName(uri));
        try (InputStream in = FeatureLoaderTest.class.getResourceAsStream(classpathResource)) {
            assertNotNull(in, "test fixture " + classpathResource + " must be on the classpath");
            Files.copy(in, cachedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
