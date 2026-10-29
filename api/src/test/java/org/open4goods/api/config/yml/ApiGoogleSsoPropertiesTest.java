package org.open4goods.api.config.yml;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/** Tests how the api Google SSO allowlist binds from configuration (GOU-1393). */
class ApiGoogleSsoPropertiesTest {

    private static final String PREFIX = "api.security.google-sso.role-allowlist";

    @Test
    void aBracketedEmailKeyKeepsItsAtSign() {
        ApiGoogleSsoProperties properties = bind(Map.of(
                PREFIX + "[Jane.Roe@Example.org][0]", "ROLE_USER",
                PREFIX + "[Jane.Roe@Example.org][1]", "XWIKIADMINGROUP"));

        assertThat(properties.rolesFor("jane.roe@example.org")).containsExactly("ROLE_USER", "XWIKIADMINGROUP");
        assertThat(properties.malformedAllowlistKeys()).isEmpty();
    }

    @Test
    void anUnbracketedEmailKeyIsReportedAsMalformed() {
        ApiGoogleSsoProperties properties = bind(Map.of(PREFIX + ".jane.roe@example.org[0]", "ROLE_USER"));

        assertThat(properties.rolesFor("jane.roe@example.org")).isEmpty();
        assertThat(properties.malformedAllowlistKeys()).hasSize(1);
    }

    @Test
    void nothingIsAllowedByDefault() {
        ApiGoogleSsoProperties properties = bind(Map.of());

        assertThat(properties.getRoleAllowlist()).isEmpty();
        assertThat(properties.rolesFor("jane.roe@example.org")).isEmpty();
        assertThat(properties.rolesFor(null)).isEmpty();
    }

    private static ApiGoogleSsoProperties bind(Map<String, String> source) {
        return new Binder(new MapConfigurationPropertySource(source))
                .bind("api.security.google-sso", Bindable.of(ApiGoogleSsoProperties.class))
                .orElseGet(ApiGoogleSsoProperties::new);
    }
}
