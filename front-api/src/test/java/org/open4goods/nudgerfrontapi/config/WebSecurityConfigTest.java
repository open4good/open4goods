package org.open4goods.nudgerfrontapi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.config.properties.ExposedDocsProperties;
import org.open4goods.nudgerfrontapi.config.properties.SecurityProperties;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Unit tests for {@link WebSecurityConfig}.
 */
class WebSecurityConfigTest {

    @Test
    void shouldCreateJwtDecoder() {
        SecurityProperties properties = new SecurityProperties();
        properties.setJwtSecret("0123456789012345678901234567890123456789012345678901234567890123");
        AuthenticationProvider authProvider = mock(AuthenticationProvider.class);
        ExposedDocsProperties exposedDocsProperties = new ExposedDocsProperties();

        JwtDecoder decoder = new WebSecurityConfig(properties, exposedDocsProperties, authProvider).jwtDecoder();

        assertThat(decoder).isNotNull();
    }
}
