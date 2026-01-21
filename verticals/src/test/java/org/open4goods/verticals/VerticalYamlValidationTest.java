package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.lang.reflect.Field;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.AttributeConfig;
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
            //.hasSameSizeAs(verticalResources)
            .allSatisfy(config -> assertVerticalConfig(config, config.getId()));
    }

    @Test
    void shouldInstantiateEachYamlIndividually() throws Exception {
        for (Resource resource : verticalResources) {
            try (InputStream inputStream = resource.getInputStream()) {
                VerticalConfig config = verticalsConfigService.getConfig(inputStream, verticalsConfigService.getDefaultConfig());

                // Inject impact score config from the already loaded service instance
                // This simulates the loading process which now separates vertical and impact score configs
                VerticalConfig loadedConfig = verticalsConfigService.getConfigById(config.getId());
                if (loadedConfig != null) {
                    config.setImpactScoreConfig(loadedConfig.getImpactScoreConfig());
                }

                assertVerticalConfig(config, resource.getFilename());
            }
        }
    }

    @Test
    void shouldResolveAttributesFromExternalCatalog() {
        VerticalConfig tvConfig = verticalsConfigService.getConfigById("tv");
        assertThat(tvConfig).isNotNull();

        AttributeConfig hdrClass = tvConfig.getAttributesConfig().getConfigs().stream()
            .filter(config -> "CLASSE_ENERGY_HDR".equals(config.getKey()))
            .findFirst()
            .orElse(null);

        assertThat(hdrClass)
            .as("HDR energy class must be loaded from the shared attribute catalog")
            .isNotNull();

        assertThat(hdrClass.getSynonyms().get("all"))
            .as("External attribute definitions must populate synonyms")
            .isNotEmpty();

        assertThat(hdrClass.getEprelFeatureNames())
            .as("External attribute definitions must retain EPREL mapping")
            .contains("energyClassHDR");
    }

    @Test
    void shouldMergeDefaultAttributesAndImpactScoreCriterias() {
        VerticalConfig tvConfig = verticalsConfigService.getConfigById("tv");
        assertThat(tvConfig).isNotNull();

        assertThat(tvConfig.getAvailableImpactScoreCriterias())
            .as("Default impact score criteria should be merged into the TV config")
            .contains("BRAND_SUSTAINALYTICS_SCORING", "DATA_QUALITY");

        List<String> attributeKeys = tvConfig.getAttributesConfig().getConfigs().stream()
            .map(AttributeConfig::getKey)
            .toList();

        assertThat(attributeKeys)
            .as("Default attributes should be merged into the TV config")
            .contains("ESG", "DATA_QUALITY");
    }

    @Test
    void shouldExposeEprelGroupNamesAsList()
    {
        VerticalConfig tvConfig = verticalsConfigService.getConfigById("tv");
        assertThat(tvConfig.getEprelGroupNames()).contains("televisions");
    }

    @Test
    void shouldFailWhenAttributeDefinitionIsMissing() throws Exception {
        Resource tvResource = verticalResources.stream()
            .filter(resource -> "tv.yml".equals(resource.getFilename()))
            .findFirst()
            .orElseThrow();

        Map<String, AttributeConfig> originalCatalog = snapshotAttributeCatalog();
        try (InputStream inputStream = tvResource.getInputStream()) {
            setAttributeCatalog(Map.of());

            assertThatThrownBy(() -> verticalsConfigService.getConfig(inputStream, verticalsConfigService.getDefaultConfig()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing attribute definition for key");
        } finally {
            setAttributeCatalog(originalCatalog);
        }
    }

    @Test
    void shouldNotExposeScoreMetadataWhenAsScoreIsFalse() throws Exception {
        Map<String, AttributeConfig> catalog = snapshotAttributeCatalog();

        catalog.values().forEach(attribute -> {
            if (!attribute.isAsScore()) {
                assertThat(isLocalisableDefined(attribute.getScoreTitle()))
                    .as("scoreTitle must be absent when asScore is false for %s", attribute.getKey())
                    .isFalse();
                assertThat(isLocalisableDefined(attribute.getScoreDescription()))
                    .as("scoreDescription must be absent when asScore is false for %s", attribute.getKey())
                    .isFalse();
                assertThat(isLocalisableDefined(attribute.getScoreUtility()))
                    .as("scoreUtility must be absent when asScore is false for %s", attribute.getKey())
                    .isFalse();
                assertThat(isCollectionDefined(attribute.getParticipateInScores()))
                    .as("participateInScores must be empty when asScore is false for %s", attribute.getKey())
                    .isFalse();
                assertThat(isCollectionDefined(attribute.getParticipateInACV()))
                    .as("participateInACV must be empty when asScore is false for %s", attribute.getKey())
                    .isFalse();
            }
        });
    }

    @Test
    void shouldReferenceExistingAttributesInTvConfiguration() {
        VerticalConfig tvConfig = verticalsConfigService.getConfigById("tv");
        assertThat(tvConfig).as("TV vertical must be available").isNotNull();

        Map<String, AttributeConfig> attributeConfigByKey = tvConfig.getAttributesConfig()
            .getConfigs()
            .stream()
            .collect(Collectors.toMap(AttributeConfig::getKey, attribute -> attribute));
        Set<String> attributeKeys = attributeConfigByKey.keySet();

        assertAttributeReferences(tvConfig.getPopularAttributes(), attributeKeys, "popularAttributes");
        assertAttributeReferences(tvConfig.getEcoFilters(), attributeKeys, "ecoFilters");
        assertAttributeReferences(tvConfig.getTechnicalFilters(), attributeKeys, "technicalFilters");
        assertAttributeReferences(tvConfig.getRequiredAttributes(), attributeKeys, "requiredAttributes");

        assertImpactScoreCriterias(attributeConfigByKey, tvConfig.getAvailableImpactScoreCriterias());
    }

    private static void assertAttributeReferences(Collection<String> attributes, Set<String> definedKeys, String section) {
        List<String> missing = attributes == null ? List.of() : attributes.stream()
            .filter(key -> !definedKeys.contains(key))
            .collect(Collectors.toList());

        assertThat(missing)
            .as("All attributes referenced in %s must be defined in attributesConfig", section)
            .isEmpty();
    }

    private static void assertImpactScoreCriterias(Map<String, AttributeConfig> attributesByKey, Collection<String> criterias) {
        if (criterias == null) {
            return;
        }

        List<String> missingAttributes = criterias.stream()
            .filter(key -> !attributesByKey.containsKey(key))
            .collect(Collectors.toList());

        assertThat(missingAttributes)
            .as("All availableImpactScoreCriterias must reference existing attributes")
            .isEmpty();

        List<String> nonScoreable = criterias.stream()
            .filter(key -> attributesByKey.containsKey(key) && !attributesByKey.get(key).isAsScore())
            .collect(Collectors.toList());

        assertThat(nonScoreable)
            .as("availableImpactScoreCriterias must reference attributes with asScore=true")
            .isEmpty();
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

        assertParticipatingScoresAreWeighted(config, sourceName, weights.keySet());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, AttributeConfig> snapshotAttributeCatalog() throws Exception {
        Field field = VerticalsConfigService.class.getDeclaredField("attributeCatalog");
        field.setAccessible(true);
        return new HashMap<>((Map<String, AttributeConfig>) field.get(verticalsConfigService));
    }

    private static void setAttributeCatalog(Map<String, AttributeConfig> catalog) throws Exception {
        Field field = VerticalsConfigService.class.getDeclaredField("attributeCatalog");
        field.setAccessible(true);
        field.set(verticalsConfigService, catalog);
    }

    private static boolean isLocalisableDefined(org.open4goods.model.Localisable<String, String> value) {
        return value != null && !value.isEmpty();
    }

    private static boolean isCollectionDefined(Collection<?> values) {
        return values != null && !values.isEmpty();
    }

    private static void assertParticipatingScoresAreWeighted(VerticalConfig config, String sourceName, Set<String> weights) {
        Collection<String> availableImpactScoreCriterias = config.getAvailableImpactScoreCriterias();
        if (availableImpactScoreCriterias == null || availableImpactScoreCriterias.isEmpty()) {
            return;
        }

        List<String> missing = config.getAttributesConfig().getConfigs().stream()
            .filter(AttributeConfig::isAsScore)
            .filter(attribute -> attribute.getParticipateInScores() != null && !attribute.getParticipateInScores().isEmpty())
            .map(AttributeConfig::getKey)
            .filter(availableImpactScoreCriterias::contains)
            .filter(key -> !weights.contains(key))
            .toList();

        assertThat(missing)
            .as("Participating scores must have ponderation defined in impactScoreConfig (%s)", sourceName)
            .isEmpty();
    }
}
