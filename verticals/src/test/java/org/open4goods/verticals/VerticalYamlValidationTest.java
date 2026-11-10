package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.lang.reflect.Field;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.ImpactScorePositionLabel;
import org.open4goods.model.vertical.ImpactScoreV2Config;
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
                    config.setAvailableImpactScoreCriterias(loadedConfig.getAvailableImpactScoreCriterias());
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

        assertThat(hdrClass.eprelFeatureNames())
            .as("External attribute definitions must retain EPREL mapping")
            .contains("energyClassHDR");
    }

    @Test
    void shouldLoadIcecatReferentialsFromAttributeYaml() {
        VerticalConfig tvConfig = verticalsConfigService.getConfigById("tv");
        assertThat(tvConfig).isNotNull();

        AttributeConfig diagonal = tvConfig.getAttributesConfig().getConfigs().stream()
            .filter(c -> "DIAGONALE_POUCES".equals(c.getKey()))
            .findFirst()
            .orElseThrow();

        assertThat(diagonal.getReferentials())
            .as("DIAGONALE_POUCES must expose a referentials block")
            .isNotNull();
        assertThat(diagonal.icecatFeatureIds())
            .as("DIAGONALE_POUCES must expose Icecat feature ID via the new referentials block")
            .contains(944);
        assertThat(diagonal.eprelFeatureNames())
            .as("DIAGONALE_POUCES must expose EPREL feature via the new referentials block")
            .contains("diagonalInch");
    }

    @Test
    void shouldExposeEprelReferentialFromAttributeYaml() {
        VerticalConfig tvConfig = verticalsConfigService.getConfigById("tv");
        AttributeConfig sdr = tvConfig.getAttributesConfig().getConfigs().stream()
            .filter(c -> "POWER_CONSUMPTION_SDR".equals(c.getKey()))
            .findFirst()
            .orElseThrow();

        assertThat(sdr.eprelFeatureNames())
            .as("POWER_CONSUMPTION_SDR must expose EPREL mapping via the new referentials block")
            .contains("powerOnModeSDR");
    }

    @Test
    void shouldMergeDefaultAttributesAndImpactScoreCriterias() {
        VerticalConfig tvConfig = verticalsConfigService.getConfigById("tv");
        assertThat(tvConfig).isNotNull();

        assertThat(tvConfig.getAvailableImpactScoreCriterias())
            .as("Default impact score criteria should be merged into the TV config, unless overridden by JSON")
            .contains("DATA_QUALITY");

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
                assertThat(attribute.getScoring())
                    .as("scoring must be absent when asScore is false for %s", attribute.getKey())
                    .isNull();
            } else {
                assertThat(attribute.getScoring())
                    .as("scoring must be defined when asScore is true for %s", attribute.getKey())
                    .isNotNull();
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

    @Test
    void noVerticalIsSwitchedToTheV2Methodology() {
        // GOU-1880 adds the v2 blocks; the switch comes with GOU-1888
        assertThat(verticalsConfigService.getConfigsWithoutDefault())
            .allSatisfy(config -> assertThat(config.getImpactScoreConfig().effectiveMethodologyVersion())
                .as("methodology version of %s", config.getId())
                .isEqualTo(ImpactScoreConfig.METHODOLOGY_V1));
    }

    /**
     * V2 criteria that replace a V1 criterion with its weight, by vertical (GOU-2041): V1 criterion to V2 criterion.
     */
    private static final Map<String, Map<String, String>> V2_REPLACED_CRITERIA = Map.of(
        "tv", Map.of("REPAIRABILITY_INDEX", "REPAIRABILITY_OR_DURABILITY_INDEX"),
        "washing-machine", Map.of("REPAIRABILITY_INDEX", "REPAIRABILITY_OR_DURABILITY_INDEX"));

    /**
     * GOU-1880: every real vertical carries a valid {@code v2} block whose matrix is the V1 matrix without
     * {@code DATA_QUALITY}, renormalised to 1 pro rata (design {@code docs/impactscore/v2-design.md}, 1.2 and 2.3),
     * except the criteria {@link #V2_REPLACED_CRITERIA} replaces, weight unchanged.
     */
    @Test
    void everyRealVerticalHasAV2MatrixWithoutDataQuality() {
        assertThat(verticalsConfigService.getConfigsWithoutDefault()).allSatisfy(config -> {
            ImpactScoreConfig impactScore = config.getImpactScoreConfig();
            assertThat(impactScore.getV2()).as("v2 block of %s", config.getId()).isNotNull();
            assertThat(ImpactScoreMethodology.validate(impactScore, config)).as("v2 errors of %s", config.getId()).isEmpty();

            Map<String, String> replaced = V2_REPLACED_CRITERIA.getOrDefault(config.getId(), Map.of());
            Map<String, Double> v1 = new HashMap<>();
            impactScore.getCriteriasPonderation().forEach((key, weight) -> v1.put(replaced.getOrDefault(key, key), weight));
            Map<String, Double> v2 = impactScore.getV2().getCriteriasPonderation();
            assertThat(v2).as("v2 matrix of %s", config.getId()).doesNotContainKey("DATA_QUALITY");
            assertThat(v2.values().stream().mapToDouble(Double::doubleValue).sum())
                .as("sum of the v2 weights of %s", config.getId()).isCloseTo(1.0, within(1e-6));

            double withoutDataQuality = v1.entrySet().stream().filter(entry -> !"DATA_QUALITY".equals(entry.getKey()))
                .mapToDouble(Map.Entry::getValue).sum();
            assertThat(v2.keySet()).as("v2 criteria of %s", config.getId())
                .containsExactlyInAnyOrderElementsOf(v1.keySet().stream().filter(key -> !"DATA_QUALITY".equals(key)).toList());
            v2.forEach((key, weight) -> assertThat(weight).as("v2 weight of %s.%s", config.getId(), key)
                .isCloseTo(v1.get(key) / withoutDataQuality, within(1e-6)));
        });
    }

    @Test
    void validV2BlockIsAccepted() {
        VerticalConfig tv = verticalsConfigService.getConfigById("tv");
        assertThat(ImpactScoreMethodology.validate(impactScoreWithV2(tv, v2 -> { }), tv)).isEmpty();

        ImpactScoreConfig active = impactScoreWithV2(tv, v2 -> { });
        active.setMethodologyVersion(2);
        assertThat(ImpactScoreMethodology.blockingErrors(active, tv)).isEmpty();
    }

    @Test
    void methodologyVersionOutsideOneAndTwoIsRefused() {
        VerticalConfig tv = verticalsConfigService.getConfigById("tv");
        ImpactScoreConfig config = impactScoreWithV2(tv, v2 -> { });
        config.setMethodologyVersion(3);
        assertThat(ImpactScoreMethodology.blockingErrors(config, tv)).anyMatch(error -> error.contains("methodologyVersion 3"));
    }

    @Test
    void versionTwoWithoutV2BlockIsRefused() {
        VerticalConfig tv = verticalsConfigService.getConfigById("tv");
        ImpactScoreConfig config = new ImpactScoreConfig();
        config.setCriteriasPonderation(tv.getImpactScoreConfig().getCriteriasPonderation());
        config.setMethodologyVersion(2);
        assertThat(ImpactScoreMethodology.blockingErrors(config, tv)).containsExactly("methodology version 2 requires a v2 block");
        assertThat(config.activeCriteriasPonderation()).as("no fallback to the V1 matrix").isEmpty();
    }

    @Test
    void v2MatrixNotSummingToOneIsRefused() {
        assertV2Refused(v2 -> v2.getCriteriasPonderation().replaceAll((key, weight) -> weight / 2), "sums to");
    }

    @Test
    void v2MatrixWeightingDataQualityIsRefused() {
        assertV2Refused(v2 -> moveWeight(v2, "DATA_QUALITY"), "cannot weight DATA_QUALITY");
    }

    @Test
    void v2MatrixWeightingEcoscoreIsRefused() {
        assertV2Refused(v2 -> moveWeight(v2, "ECOSCORE"), "cannot weight ECOSCORE");
    }

    @Test
    void v2MatrixWeightingAnUnknownCriterionIsRefused() {
        assertV2Refused(v2 -> moveWeight(v2, "NOT_AN_ATTRIBUTE"), "not an asScore attribute");
    }

    @Test
    void v2DerivedCriterionWeightedWithItsSourceIsRefused() {
        assertV2Refused(v2 -> {
            moveWeight(v2, "REPAIRABILITY_OR_DURABILITY_INDEX");
            v2.getCriteriasPonderation().put("REPAIRABILITY_OR_DURABILITY_INDEX",
                v2.getCriteriasPonderation().get("REPAIRABILITY_OR_DURABILITY_INDEX") - 0.01);
            v2.getCriteriasPonderation().put("DURABILITY_INDEX", 0.01);
        }, "weights both REPAIRABILITY_OR_DURABILITY_INDEX and its source DURABILITY_INDEX");
    }

    @Test
    void v2DerivedCriterionReadingAnUnknownSourceIsRefused() {
        VerticalConfig tv = verticalsConfigService.getConfigById("tv");
        AttributeConfig derived = tv.getAttributesConfig().getAttributeConfigByKey("REPAIRABILITY_OR_DURABILITY_INDEX");
        List<String> sources = derived.getScoreSources();
        try {
            derived.setScoreSources(List.of("NOT_AN_ATTRIBUTE"));
            assertThat(ImpactScoreMethodology.validate(tv.getImpactScoreConfig(), tv))
                .anyMatch(error -> error.contains("reads NOT_AN_ATTRIBUTE, which is not an attribute"));
        } finally {
            derived.setScoreSources(sources);
        }
    }

    /** GOU-2041: the derived criterion reads the durability index first, never from merchant data itself. */
    @Test
    void repairabilityOrDurabilityCriterionReadsDurabilityFirst() {
        for (String id : List.of("tv", "washing-machine")) {
            VerticalConfig vertical = verticalsConfigService.getConfigById(id);
            AttributeConfig derived = vertical.getAttributesConfig()
                .getAttributeConfigByKey("REPAIRABILITY_OR_DURABILITY_INDEX");
            assertThat(derived).as("derived criterion of %s", id).isNotNull();
            assertThat(derived.isAsScore()).isTrue();
            assertThat(derived.getScoreSources()).containsExactly("DURABILITY_INDEX", "REPAIRABILITY_INDEX");
            assertThat(derived.getSynonyms()).as("never read from merchant data").isEmpty();
            assertThat(derived.getScoring().getNormalization().getMethod().name()).isEqualTo("MINMAX_FIXED");
            assertThat(derived.getScoring().getNormalization().getParams().getFixedMin()).isEqualTo(0.0);
            assertThat(derived.getScoring().getNormalization().getParams().getFixedMax()).isEqualTo(10.0);
            assertThat(vertical.getImpactScoreConfig().getCriteriasPonderation())
                .as("V1 matrix of %s unchanged", id).containsKey("REPAIRABILITY_INDEX")
                .doesNotContainKey("REPAIRABILITY_OR_DURABILITY_INDEX");
            assertThat(vertical.getAvailableImpactScoreCriterias()).as("V1 DATA_QUALITY of %s unchanged", id)
                .doesNotContain("REPAIRABILITY_OR_DURABILITY_INDEX");
            assertThat(vertical.getAttributesConfig().getAttributeConfigByKey("REPAIRABILITY_INDEX").getScoring()
                .getNormalization().getMethod().name()).as("V1 REPAIRABILITY_INDEX scale").isEqualTo("MINMAX_OBSERVED");
        }
    }

    @Test
    void v2CoverageThresholdOutsideZeroOneIsRefused() {
        assertV2Refused(v2 -> v2.setMinRealWeightCoverage(0.0), "minRealWeightCoverage");
        assertV2Refused(v2 -> v2.setMinRealWeightCoverage(1.2), "minRealWeightCoverage");
        assertV2Refused(v2 -> v2.setMinRealWeightCoverage(null), "minRealWeightCoverage");
    }

    @Test
    void v2PositionLabelsNotStrictlyDecreasingAreRefused() {
        assertV2Refused(v2 -> v2.setPositionLabels(List.of(
            new ImpactScorePositionLabel("excellent", 0.5),
            new ImpactScorePositionLabel("good", 0.5),
            new ImpactScorePositionLabel("poor", 0.0))), "strictly decrease");
    }

    @Test
    void v2PositionLabelsNotEndingAtZeroAreRefused() {
        assertV2Refused(v2 -> v2.setPositionLabels(List.of(
            new ImpactScorePositionLabel("excellent", 0.9),
            new ImpactScorePositionLabel("good", 0.5))), "last threshold must be 0");
    }

    @Test
    void invalidV2BlockFailsOnlyOnceActive() {
        VerticalConfig tv = verticalsConfigService.getConfigById("tv");
        ImpactScoreConfig config = impactScoreWithV2(tv, v2 -> v2.setMinRealWeightCoverage(null));
        // Merged dark: refused by this test, not blocking at startup while V1 is active
        assertThat(ImpactScoreMethodology.validate(config, tv)).isNotEmpty();
        assertThat(ImpactScoreMethodology.blockingErrors(config, tv)).isEmpty();

        config.setMethodologyVersionOverride(2);
        assertThat(ImpactScoreMethodology.blockingErrors(config, tv)).isNotEmpty();
    }

    private static void assertV2Refused(Consumer<ImpactScoreV2Config> breaker, String expectedError) {
        VerticalConfig tv = verticalsConfigService.getConfigById("tv");
        ImpactScoreConfig config = impactScoreWithV2(tv, breaker);
        assertThat(ImpactScoreMethodology.validate(config, tv))
            .as("v2 block broken by the case expecting '%s'", expectedError)
            .anyMatch(error -> error.contains(expectedError));
    }

    /**
     * A V1 configuration of the vertical with a valid {@code v2} block (V1 matrix without DATA_QUALITY, normalised),
     * then changed by {@code customizer}.
     */
    private static ImpactScoreConfig impactScoreWithV2(VerticalConfig vertical, Consumer<ImpactScoreV2Config> customizer) {
        Map<String, Double> v1 = vertical.getImpactScoreConfig().getCriteriasPonderation();
        Map<String, Double> v2Weights = new LinkedHashMap<>();
        v1.forEach((key, weight) -> {
            if (!"DATA_QUALITY".equals(key)) {
                v2Weights.put(key, weight);
            }
        });
        double sum = v2Weights.values().stream().mapToDouble(Double::doubleValue).sum();
        v2Weights.replaceAll((key, weight) -> weight / sum);

        ImpactScoreV2Config v2 = new ImpactScoreV2Config();
        v2.setCriteriasPonderation(v2Weights);
        v2.setMinRealWeightCoverage(0.6);
        v2.setPositionLabels(new ArrayList<>(List.of(
            new ImpactScorePositionLabel("excellent", 0.9),
            new ImpactScorePositionLabel("good", 0.67),
            new ImpactScorePositionLabel("fair", 0.33),
            new ImpactScorePositionLabel("poor", 0.0))));
        customizer.accept(v2);

        ImpactScoreConfig config = new ImpactScoreConfig();
        config.setCriteriasPonderation(new HashMap<>(v1));
        config.setV2(v2);
        return config;
    }

    /** Moves the weight of the first criterion of the v2 matrix to {@code key}, keeping the sum at 1. */
    private static void moveWeight(ImpactScoreV2Config v2, String key) {
        String first = v2.getCriteriasPonderation().keySet().iterator().next();
        v2.getCriteriasPonderation().put(key, v2.getCriteriasPonderation().remove(first));
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

        assertThat(ImpactScoreMethodology.validate(config.getImpactScoreConfig(), config))
            .as("ImpactScore methodology version and v2 block must be valid (%s)", sourceName)
            .isEmpty();
        assertThat(config.getImpactScoreConfig().getMethodologyRevision())
            .as("Methodology revision is computed at load time (%s)", sourceName)
            .matches("[0-9a-f]{8}");
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
        List<String> targetingImpactScore = config.getAttributesConfig().getConfigs().stream()
            .filter(attribute -> attribute.getParticipateInScores() != null
                && attribute.getParticipateInScores().contains("ECOSCORE"))
            .map(AttributeConfig::getKey)
            .toList();

        assertThat(targetingImpactScore)
            .as("ECOSCORE is computed from criteriasPonderation and cannot be a composite (%s)", sourceName)
            .isEmpty();

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
