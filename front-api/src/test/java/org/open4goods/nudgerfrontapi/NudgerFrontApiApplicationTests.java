package org.open4goods.nudgerfrontapi;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHRepository;
import org.open4goods.brand.service.BrandService;
import org.open4goods.icecat.repository.IcecatCategoryRepository;
import org.open4goods.icecat.repository.IcecatFeatureGroupRepository;
import org.open4goods.icecat.repository.IcecatFeatureRepository;
import org.open4goods.icecat.repository.IcecatSupplierRepository;
import org.open4goods.services.geocode.service.IpGeolocationService;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class NudgerFrontApiApplicationTests
{

    @MockitoBean
    private BrandService brandService;

    @MockitoBean
    private GHRepository ghRepository;

    @MockitoBean
    private IpGeolocationService ipGeolocationService;

    // This context-load-only test carries no real Elasticsearch connection (no spring.elasticsearch.*
    // in the base application.yml), and these Spring Data repository proxies are eagerly
    // instantiated regardless of @Lazy on their consumers, so they need a stand-in here.
    @MockitoBean
    private IcecatFeatureRepository icecatFeatureRepository;

    @MockitoBean
    private IcecatCategoryRepository icecatCategoryRepository;

    @MockitoBean
    private IcecatFeatureGroupRepository icecatFeatureGroupRepository;

    @MockitoBean
    private IcecatSupplierRepository icecatSupplierRepository;

    @Test
    void contextLoads()
    {
    }

}
