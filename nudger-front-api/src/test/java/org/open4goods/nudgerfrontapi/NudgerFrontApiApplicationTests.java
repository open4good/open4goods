package org.open4goods.nudgerfrontapi;

import org.junit.jupiter.api.Test;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class NudgerFrontApiApplicationTests {

    @MockBean
    ProductRepository productRepository;

    @Test
    void contextLoads() {
        // Context load test
    }
}
