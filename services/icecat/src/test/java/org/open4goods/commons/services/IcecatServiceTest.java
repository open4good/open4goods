package org.open4goods.commons.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.services.IcecatFileDownloadService;
import org.open4goods.icecat.services.IcecatIndexService;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.icecat.services.loader.CategoryLoader;
import org.open4goods.icecat.services.loader.FeatureLoader;
import org.open4goods.model.Localisable;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.FeatureGroup;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.brand.service.BrandService;
import org.open4goods.verticals.VerticalsConfigService;

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

        FeatureLoader fl = new FeatureLoader(cfg, downloader, brand);
        CategoryLoader cl = new CategoryLoader(cfg, downloader, vertical, fl);
        IcecatIndexService indexService = Mockito.mock(IcecatIndexService.class);

        assertDoesNotThrow(() -> new IcecatService(cfg, downloader, fl, cl, indexService));
    }

    @Test
    public void testFeaturesWithNullAttributeValue() {
        IcecatConfiguration cfg = new IcecatConfiguration();
        IcecatFileDownloadService downloader = mockDownloader();
        FeatureLoader fl = Mockito.mock(FeatureLoader.class);
        CategoryLoader cl = Mockito.mock(CategoryLoader.class);
        IcecatIndexService indexService = Mockito.mock(IcecatIndexService.class);

        IcecatService service = new IcecatService(cfg, downloader, fl, cl, indexService);

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

        Mockito.when(indexService.findFeature(featureId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.features(verticalConfig, language, product));
    }
}
