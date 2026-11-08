package org.open4goods.nudgerfrontapi;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHRepository;
import org.open4goods.brand.service.BrandService;
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

    // Icecat*Repository proxies no longer need a stand-in here: LocalDevConfig's
    // elasticsearchOperations() stub now answers getElasticsearchConverter().getMappingContext()
    // with a real SimpleElasticsearchMappingContext, which is all
    // @EnableElasticsearchRepositories needs to create the repository proxies without a real
    // Elasticsearch connection (this test runs under the "local" profile, which is front-api's
    // default profile).

    @Test
    void contextLoads()
    {
    }

}
