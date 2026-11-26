package org.open4goods.ui.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * {@code open-data-config.redirect-base-url} keeps its name after ui dropped the {@code opendata} dependency
 * (GOU-2290): it is the property rendered in ui's config tree by {@code render-service-secrets.sh} (GOU-2247).
 */
class OpenDataRedirectPropertiesTest {

    @Test
    void bindsTheRenderedPropertyName() {
        Binder binder = new Binder(new MapConfigurationPropertySource(
                Map.of("open-data-config.redirect-base-url", "https://data.nudger.fr")));

        OpenDataRedirectProperties properties = binder
                .bind("open-data-config", Bindable.ofInstance(new OpenDataRedirectProperties())).get();

        assertThat(properties.getRedirectBaseUrl()).isEqualTo("https://data.nudger.fr");
        assertThat(properties.redirectUrl("gtin-open-data.zip")).isEqualTo("https://data.nudger.fr/gtin-open-data.zip");
    }

    @Test
    void emptyByDefault() {
        OpenDataRedirectProperties properties = new OpenDataRedirectProperties();

        assertThat(properties.getRedirectBaseUrl()).isEmpty();
        assertThat(properties.redirectUrl("gtin-open-data.zip")).isNull();
    }
}
