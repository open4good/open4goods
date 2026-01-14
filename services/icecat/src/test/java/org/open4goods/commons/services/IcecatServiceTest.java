package org.open4goods.commons.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.model.IcecatFeature;
import org.open4goods.icecat.model.IcecatName;
import org.open4goods.icecat.model.IcecatNames;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.icecat.services.loader.CategoryLoader;
import org.open4goods.icecat.services.loader.FeatureLoader;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.brand.service.BrandService;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.verticals.VerticalsConfigService;

import org.open4goods.model.Localisable;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.FeatureGroup;
import org.open4goods.model.vertical.VerticalConfig;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class IcecatServiceTest {

    @Test
    public void testConstructorDoesNotThrow() {
        IcecatConfiguration cfg = new IcecatConfiguration();
        RemoteFileCachingService cache = Mockito.mock(RemoteFileCachingService.class);
        BrandService brand = Mockito.mock(BrandService.class);
        VerticalsConfigService vertical = Mockito.mock(VerticalsConfigService.class);

        FeatureLoader fl = new FeatureLoader(new XmlMapper(), cfg, cache, ".", brand);
        CategoryLoader cl = new CategoryLoader(new XmlMapper(), cfg, cache, ".", vertical, fl);

        assertDoesNotThrow(() -> new IcecatService(new XmlMapper(), cfg, cache, ".", fl, cl));
    }

    @Test
    public void testGetOriginalEnglishName() throws Exception {
        IcecatConfiguration cfg = new IcecatConfiguration();
        RemoteFileCachingService cache = Mockito.mock(RemoteFileCachingService.class);
        BrandService brand = Mockito.mock(BrandService.class);
        VerticalsConfigService vertical = Mockito.mock(VerticalsConfigService.class);

        FeatureLoader fl = new FeatureLoader(new XmlMapper(), cfg, cache, ".", brand);
        CategoryLoader cl = new CategoryLoader(new XmlMapper(), cfg, cache, ".", vertical, fl);

        IcecatService service = new IcecatService(new XmlMapper(), cfg, cache, ".", fl, cl);

        IcecatName nameEn = new IcecatName();
        nameEn.setLangId(1);
        nameEn.setTextValue("Color");
        IcecatNames names = new IcecatNames();
        names.setNames(Arrays.asList(nameEn));

        IcecatFeature feature = new IcecatFeature();
        feature.setID("1");
        feature.setNames(names);

        Map<Integer, IcecatFeature> map = fl.getFeaturesById();
        map.put(1, feature);

        fl.getFeaturesByNames().put(IdHelper.normalizeAttributeName("Color"), Collections.singleton(1));

        String resolved = service.getOriginalEnglishName("Color", null);
        assertEquals("Color", resolved);
    }


    @Test
    public void testFeaturesWithNullAttributeValue() {
        // Setup Mocks
        IcecatConfiguration cfg = new IcecatConfiguration();
        RemoteFileCachingService cache = Mockito.mock(RemoteFileCachingService.class);
        BrandService brand = Mockito.mock(BrandService.class);
        VerticalsConfigService verticalService = Mockito.mock(VerticalsConfigService.class);
        FeatureLoader fl = Mockito.mock(FeatureLoader.class);
        CategoryLoader cl = Mockito.mock(CategoryLoader.class);

        IcecatService service = new IcecatService(new XmlMapper(), cfg, cache, ".", fl, cl);

        // Setup Data
        int featureId = 123;
        String language = "fr";
        
        // Mock VerticalConfig
        VerticalConfig verticalConfig = Mockito.mock(VerticalConfig.class);
        FeatureGroup featureGroup = new FeatureGroup();
        featureGroup.setFeaturesId(Collections.singletonList(featureId));
        Localisable<String, String> groupName = new Localisable<>();
        groupName.put("fr", "Group Name");
        featureGroup.setName(groupName);
        Mockito.when(verticalConfig.getFeatureGroups()).thenReturn(Collections.singletonList(featureGroup));

        // Mock Product and Attribute
        Product product = Mockito.mock(Product.class);
        org.open4goods.model.attribute.ProductAttributes attributes = Mockito.mock(org.open4goods.model.attribute.ProductAttributes.class);
        org.open4goods.model.attribute.ProductAttribute attribute = new org.open4goods.model.attribute.ProductAttribute();
        attribute.setValue(null); // The cause of NPE
        
        Mockito.when(product.getAttributes()).thenReturn(attributes);
        Mockito.when(attributes.attributeByFeatureId(featureId)).thenReturn(attribute);

        // Mock FeatureLoader behavior
        IcecatFeature icecatFeature = new IcecatFeature();
        IcecatNames names = new IcecatNames();
        names.setNames(Collections.emptyList());
        icecatFeature.setNames(names);
        
        Map<Integer, IcecatFeature> featuresMap = Mockito.mock(Map.class);
        Mockito.when(fl.getFeaturesById()).thenReturn(featuresMap);
        Mockito.when(featuresMap.get(featureId)).thenReturn(icecatFeature);

        // Execute and Assert
        // This should NOT throw NPE after fix
        assertDoesNotThrow(() -> service.features(verticalConfig, language, product));
    }
}
