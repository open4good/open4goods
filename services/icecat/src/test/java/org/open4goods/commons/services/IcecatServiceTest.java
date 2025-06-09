package org.open4goods.commons.services;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.commons.config.yml.IcecatConfiguration;
import org.open4goods.commons.services.BrandService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.icecat.IcecatFeature;
import org.open4goods.model.icecat.IcecatName;
import org.open4goods.model.icecat.IcecatNames;

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
}
