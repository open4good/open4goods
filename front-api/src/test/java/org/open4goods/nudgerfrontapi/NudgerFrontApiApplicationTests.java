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

    @Test
    void contextLoads()
    {
    }

}
