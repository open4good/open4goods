package org.open4goods.nudgerfrontapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "front.cache.path=${java.io.tmpdir}")
class NudgerFrontApiApplicationTests {

    @Test
    void contextLoads() {
        // Context load test
    }
}
