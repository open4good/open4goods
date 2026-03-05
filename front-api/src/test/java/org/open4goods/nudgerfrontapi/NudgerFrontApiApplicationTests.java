package org.open4goods.nudgerfrontapi;

import org.junit.jupiter.api.Test;
import org.open4goods.brand.service.BrandService;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@SpringBootTest

class NudgerFrontApiApplicationTests {

        @MockBean
        private BrandService brandService;

        @Test
        void contextLoads() {
        }

}
