package org.open4goods.sbadmin;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full Spring context boot test. Guards against wiring regressions such as the removed
 * {@code AuthenticationProvider} dependency leaving a bean unresolved - a plain
 * {@code mvn compile} would not catch that.
 */
@SpringBootTest
class SbAdminApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: a failing context refresh fails this test.
        // Google SSO is unconfigured here (no client-id), so oauth2Login must not be wired
        // and the context must still boot - see the GoogleSsoConfigured nested test for the
        // opposite case.
    }

    @Nested
    @SpringBootTest(properties = {
            "spring.security.oauth2.client.registration.google.client-id=test-client-id",
            "spring.security.oauth2.client.registration.google.client-secret=test-client-secret"
    })
    class GoogleSsoConfigured {

        @Test
        void contextLoadsWithOauth2LoginWired() {
            // Intentionally empty: a failing context refresh fails this test. With credentials
            // present, oauth2Login() is wired and must not throw during filter chain assembly.
        }
    }
}
