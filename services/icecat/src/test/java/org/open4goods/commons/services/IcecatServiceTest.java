package org.open4goods.commons.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.open4goods.brand.service.BrandService;
import org.open4goods.icecat.client.IcecatHttpClient;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.model.IcecatFeature;
import org.open4goods.icecat.model.IcecatName;
import org.open4goods.icecat.model.IcecatNames;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.icecat.services.loader.CategoryLoader;
import org.open4goods.icecat.services.loader.FeatureLoader;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.verticals.VerticalsConfigService;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Unit tests for IcecatService.
 */
public class IcecatServiceTest {

    @TempDir
    Path tempDir;

    private IcecatConfiguration config;
    private IcecatHttpClient httpClient;
    private BrandService brandService;
    private VerticalsConfigService verticalsConfigService;
    private XmlMapper xmlMapper;

    @BeforeEach
    void setUp() {
        config = new IcecatConfiguration();
        httpClient = Mockito.mock(IcecatHttpClient.class);
        brandService = Mockito.mock(BrandService.class);
        verticalsConfigService = Mockito.mock(VerticalsConfigService.class);
        xmlMapper = new XmlMapper();
    }

    @Test
    public void testConstructorDoesNotThrow() {
        FeatureLoader fl = new FeatureLoader(xmlMapper, config, httpClient, brandService);
        CategoryLoader cl = new CategoryLoader(xmlMapper, config, httpClient, tempDir.toString(), verticalsConfigService, fl);

        assertDoesNotThrow(() -> new IcecatService(config, httpClient, fl, cl));
    }

    @Test
    public void testGetOriginalEnglishName() throws Exception {
        FeatureLoader fl = new FeatureLoader(xmlMapper, config, httpClient, brandService);
        CategoryLoader cl = new CategoryLoader(xmlMapper, config, httpClient, tempDir.toString(), verticalsConfigService, fl);

        IcecatService service = new IcecatService(config, httpClient, fl, cl);

        // Create a test feature with English name
        IcecatName nameEn = new IcecatName();
        nameEn.setLangId(1);
        nameEn.setTextValue("Color");
        IcecatNames names = new IcecatNames();
        names.setNames(Arrays.asList(nameEn));

        IcecatFeature feature = new IcecatFeature();
        feature.setID("1");
        feature.setNames(names);

        // Add the feature to the loader's map
        Map<Integer, IcecatFeature> map = fl.getFeaturesById();
        map.put(1, feature);

        fl.getFeaturesByNames().put(IdHelper.normalizeAttributeName("Color"), Collections.singleton(1));

        String resolved = service.getOriginalEnglishName("Color", null);
        assertEquals("Color", resolved);
    }

    @Test
    public void testResolveFeatureName() {
        FeatureLoader fl = new FeatureLoader(xmlMapper, config, httpClient, brandService);
        CategoryLoader cl = new CategoryLoader(xmlMapper, config, httpClient, tempDir.toString(), verticalsConfigService, fl);

        IcecatService service = new IcecatService(config, httpClient, fl, cl);

        // Add a feature name mapping
        fl.getFeaturesByNames().put(IdHelper.normalizeAttributeName("Screen Size"), Collections.singleton(42));

        var result = service.resolveFeatureName("Screen Size");
        assertEquals(Collections.singleton(42), result);
    }

    @Test
    public void testGetFeatureNameReturnsUnsolved() {
        FeatureLoader fl = new FeatureLoader(xmlMapper, config, httpClient, brandService);
        CategoryLoader cl = new CategoryLoader(xmlMapper, config, httpClient, tempDir.toString(), verticalsConfigService, fl);

        IcecatService service = new IcecatService(config, httpClient, fl, cl);

        // No features loaded, should return "Unsolved" message
        String result = service.getFeatureName(999, "en");
        assertEquals("Unsolved: 999,1", result);
    }
}
