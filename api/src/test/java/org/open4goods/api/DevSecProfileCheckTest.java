package org.open4goods.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("devsec")
class DevSecProfileCheckTest {

    @Test
    void contextLoads() {
        // This test ensures that the application context loads successfully with the 'devsec' profile.
    }
}
