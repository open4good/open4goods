package org.open4goods.b2b;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "front.cache.path=${java.io.tmpdir}")
class B2bApplicationTests {

    @Test
    void contextLoads() {
        // Context load test
    }
}
