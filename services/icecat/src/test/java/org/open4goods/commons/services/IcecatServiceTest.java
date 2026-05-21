package org.open4goods.commons.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.model.IcecatFeature;
import org.open4goods.icecat.model.IcecatNames;
import org.open4goods.icecat.services.IcecatFileDownloadService;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.icecat.services.loader.CategoryLoader;
import org.open4goods.icecat.services.loader.FeatureLoader;
import org.open4goods.model.Localisable;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.FeatureGroup;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.brand.service.BrandService;
import org.open4goods.verticals.VerticalsConfigService;

import tools.jackson.dataformat.xml.XmlMapper;

public class IcecatServiceTest {

    /** Builds a mocked IcecatFileDownloadService for unit tests (no actual downloads). */
    private static IcecatFileDownloadService mockDownloader() {
        IcecatConfiguration cfg = new IcecatConfiguration();
        return new IcecatFileDownloadService(
                Mockito.mock(org.open4goods.services.remotefilecaching.service.RemoteFileCachingService.class),
                ".",
                cfg);
    }

    @Test
    public void testConstructorDoesNotThrow() {
        IcecatConfiguration cfg = new IcecatConfiguration();
        BrandService brand = Mockito.mock(BrandService.class);
        VerticalsConfigService vertical = Mockito.mock(VerticalsConfigService.class);
        IcecatFileDownloadService downloader = mockDownloader();

        FeatureLoader fl = new FeatureLoader(new XmlMapper(), cfg, downloader, brand);
        CategoryLoader cl = new CategoryLoader(new XmlMapper(), cfg, downloader, vertical, fl);

        assertDoesNotThrow(() -> new IcecatService(new XmlMapper(), cfg, downloader, fl, cl));
    }

    @Test
    public void testFeaturesWithNullAttributeValue() {
        IcecatConfiguration cfg = new IcecatConfiguration();
        IcecatFileDownloadService downloader = mockDownloader();
        FeatureLoader fl = Mockito.mock(FeatureLoader.class);
        CategoryLoader cl = Mockito.mock(CategoryLoader.class);

        IcecatService service = new IcecatService(new XmlMapper(), cfg, downloader, fl, cl);

        int featureId = 123;
        String language = "fr";

        VerticalConfig verticalConfig = Mockito.mock(VerticalConfig.class);
        FeatureGroup featureGroup = new FeatureGroup();
        featureGroup.setFeaturesId(Collections.singletonList(featureId));
        Localisable<String, String> groupName = new Localisable<>();
        groupName.put("fr", "Group Name");
        featureGroup.setName(groupName);
        Mockito.when(verticalConfig.getFeatureGroups()).thenReturn(Collections.singletonList(featureGroup));

        Product product = Mockito.mock(Product.class);
        org.open4goods.model.attribute.ProductAttributes attributes =
                Mockito.mock(org.open4goods.model.attribute.ProductAttributes.class);
        org.open4goods.model.attribute.ProductAttribute attribute =
                new org.open4goods.model.attribute.ProductAttribute();
        attribute.setValue(null);

        Mockito.when(product.getAttributes()).thenReturn(attributes);
        Mockito.when(attributes.attributeByFeatureId(featureId)).thenReturn(attribute);

        IcecatFeature icecatFeature = new IcecatFeature();
        IcecatNames icecatNames = new IcecatNames();
        icecatNames.setNames(Collections.emptyList());
        icecatFeature.setNames(icecatNames);

        Map<Integer, IcecatFeature> featuresMap = Mockito.mock(Map.class);
        Mockito.when(fl.getFeaturesById()).thenReturn(featuresMap);
        Mockito.when(featuresMap.get(featureId)).thenReturn(icecatFeature);

        assertDoesNotThrow(() -> service.features(verticalConfig, language, product));
    }
}
