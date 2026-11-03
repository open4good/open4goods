package org.open4goods.nudgerfrontapi.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;

/**
 * Binds {@link GoogleSsoProperties} from real YAML, as Spring Boot does at
 * startup, to pin down how e-mail keys of the role allowlist survive binding.
 * <p>
 * Setting the map through the setter (as the service tests do) hides the
 * binding pitfall: an unbracketed YAML key loses its {@code @}.
 * </p>
 */
class GoogleSsoPropertiesBindingTest {

    private static final String PREFIX = "front.security.google-sso";

    @Test
    void bracketedEmailKeyIsResolvedByRolesFor() throws Exception {
        GoogleSsoProperties properties = bind("""
                front:
                  security:
                    google-sso:
                      enabled: true
                      role-allowlist:
                        "[john.doe@example.com]": [ROLE_SITEEDITOR, XWIKIADMINGROUP]
                """);

        assertThat(properties.rolesFor("john.doe@example.com"))
                .containsExactly("ROLE_SITEEDITOR", "XWIKIADMINGROUP");
        assertThat(properties.malformedAllowlistKeys()).isEmpty();
    }

    @Test
    void bracketedKeyIsNormalizedToLowerCase() throws Exception {
        GoogleSsoProperties properties = bind("""
                front:
                  security:
                    google-sso:
                      role-allowlist:
                        "[John.Doe@Example.COM]": [ROLE_SITEEDITOR]
                """);

        assertThat(properties.getRoleAllowlist()).containsOnlyKeys("john.doe@example.com");
        assertThat(properties.rolesFor("JOHN.DOE@example.com")).containsExactly("ROLE_SITEEDITOR");
    }

    @Test
    void unbracketedKeyLosesItsAtSignAndIsDetected() throws Exception {
        GoogleSsoProperties properties = bind("""
                front:
                  security:
                    google-sso:
                      enabled: true
                      role-allowlist:
                        john.doe@example.com: [ROLE_SITEEDITOR]
                """);

        assertThat(properties.rolesFor("john.doe@example.com")).isEmpty();
        assertThat(properties.malformedAllowlistKeys()).containsExactly("john.doeexample.com");
    }

    private static GoogleSsoProperties bind(String yaml) throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("test", new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8)));
        Binder binder = new Binder(ConfigurationPropertySources.from(sources));
        return binder.bindOrCreate(PREFIX, Bindable.of(GoogleSsoProperties.class));
    }
}
