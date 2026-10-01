package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * Reproduces the GOU-173 binding gap: application.yml declares the per-network
 * feed scheduling under {@code feed.providers.<network>.*}, but every reader of
 * that configuration (the {@code @Scheduled(cron = "${feed.<network>.cron:-}")}
 * annotations in the network feed services, and {@code FeedConfiguration}'s
 * {@code awin}/{@code effiliation}/... nested fields bound at the {@code feed}
 * prefix) only ever looks at {@code feed.<network>.*} directly. The
 * {@code feed.providers.*} sub-tree is therefore silently ignored.
 *
 * <p>This test loads the real {@code api/src/main/resources/application.yml} and
 * asserts that the resolved value of {@code feed.<network>.cron} (the property the
 * schedulers actually read) is a real cron expression rather than the disabling
 * sentinel. It fails while the YAML nests these keys under {@code providers:} and
 * passes once the YAML is aligned to the flat {@code feed.<network>.*} form that
 * the code already reads everywhere else.
 */
class FeedProvidersConfigBindingTest {

    private static final List<String> NETWORKS = List.of(
            "awin", "effiliation", "tradetracker", "kwanko", "webgains", "cj");

    private StandardEnvironment loadApplicationYml() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load("application.yml",
                new ClassPathResource("application.yml"));
        StandardEnvironment environment = new StandardEnvironment();
        for (PropertySource<?> source : sources) {
            environment.getPropertySources().addLast(source);
        }
        return environment;
    }

    @Test
    void feedNetworkCronIsResolvedDirectlyUnderFeedPrefix() throws IOException {
        StandardEnvironment environment = loadApplicationYml();

        for (String network : NETWORKS) {
            String property = "feed." + network + ".cron";
            String value = environment.getProperty(property);

            assertThat(value)
                    .as("'%s' must resolve to a real cron expression, not be left unset "
                            + "(which makes the @Scheduled annotation on the %s feed service "
                            + "resolve its '${%s:-}' placeholder to the disabling '-' sentinel)",
                            property, network, property)
                    .isNotNull()
                    .isNotBlank();
        }
    }

    @Test
    void feedProvidersSubtreeIsNotTheBoundForm() throws IOException {
        StandardEnvironment environment = loadApplicationYml();

        // Documents the historical, non-functional shape so a future re-introduction
        // of the "providers:" nesting is caught: it must not be the only place a
        // network's cron is declared.
        PropertySource<?> yamlSource = environment.getPropertySources().stream()
                .filter(MapPropertySource.class::isInstance)
                .findFirst()
                .orElseThrow();
        Object providersAwinCron = ((MapPropertySource) yamlSource).getProperty("feed.providers.awin.cron");

        if (providersAwinCron != null) {
            assertThat(environment.getProperty("feed.awin.cron"))
                    .as("feed.providers.awin.cron is declared but nothing reads it; "
                            + "feed.awin.cron must carry the real value instead")
                    .isEqualTo(providersAwinCron);
        }
    }
}
