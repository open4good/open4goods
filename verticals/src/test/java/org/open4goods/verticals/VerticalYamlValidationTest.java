package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import static org.mockito.Mockito.mock;

/**
 * Validates that every vertical YAML file can be instantiated into a {@link VerticalConfig}
 * and that their impact score weights are well-defined.
 */
class VerticalYamlValidationTest {

    private static final String DEFAULT_CONFIG_FILENAME = "_default.yml";
    private static final String CLASSPATH_VERTICALS = "classpath:/verticals/*.yml";

    private static VerticalsConfigService verticalsConfigService;
    private static List<Resource> verticalResources;

    @BeforeAll
    static void setUp() throws Exception {
        SerialisationService serialisationService = new SerialisationService();
        GoogleTaxonomyService googleTaxonomyService = mock(GoogleTaxonomyService.class);
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

        verticalsConfigService = new VerticalsConfigService(serialisationService, googleTaxonomyService, resourceResolver);
        verticalResources = loadVerticalResources(resourceResolver);

        assertThat(verticalsConfigService.getDefaultConfig()).as("Default config must load").isNotNull();
        assertThat(verticalResources).as("At least one vertical YAML must be present").isNotEmpty();
    }

    private static List<Resource> loadVerticalResources(ResourcePatternResolver resolver) throws IOException {
        return Arrays.stream(resolver.getResources(CLASSPATH_VERTICALS))
            .filter(resource -> !DEFAULT_CONFIG_FILENAME.equals(resource.getFilename()))
            .collect(Collectors.toList());
    }

    @Test
    void shouldLoadSameNumberOfConfigsAsYamlFiles() {
        assertThat(verticalsConfigService.getConfigsWithoutDefault())
            .as("Each YAML file should be represented in VerticalsConfigService")
            .hasSameSizeAs(verticalResources)
            .allSatisfy(config -> assertVerticalConfig(config, config.getId()));
    }

    @Test
    void shouldInstantiateEachYamlIndividually() throws Exception {
        for (Resource resource : verticalResources) {
            try (InputStream inputStream = resource.getInputStream()) {
                VerticalConfig config = verticalsConfigService.getConfig(inputStream, verticalsConfigService.getDefaultConfig());
                assertVerticalConfig(config, resource.getFilename());
            }
        }
    }

    private static void assertVerticalConfig(VerticalConfig config, String sourceName) {
        assertThat(config).as("Vertical config should not be null for %s", sourceName).isNotNull();
        assertThat(config.getId()).as("Each vertical needs an ID (%s)", sourceName).isNotBlank();
        assertThat(config.getImpactScoreConfig())
            .as("Impact score configuration is required (%s)", sourceName)
            .isNotNull();

        Map<String, Double> weights = config.getImpactScoreConfig().getCriteriasPonderation();
        assertThat(weights)
            .as("Impact score weights must be defined (%s)", sourceName)
            .isNotNull()
            .isNotEmpty();

        assertThat(weights.entrySet())
            .as("Impact score weights must contain names and values (%s)", sourceName)
            .allSatisfy(entry -> {
                assertThat(entry.getKey()).as("Criterion name should be provided (%s)", sourceName).isNotBlank();
                assertThat(entry.getValue()).as("Criterion weight must be provided (%s)", sourceName).isNotNull();
            });

        double total = weights.values().stream()
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .sum();

        assertThat(total)
            .as("Impact score weights must sum to 1 (%s)", sourceName)
            .isCloseTo(1.0d, within(1e-6));
    }
}
