package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.referential.EprelFeatureReferential;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.mockito.Mockito.mock;

/**
 * GOU-1897 point 2: a source label must feed one attribute only. {@code AttributesConfig#synonyms()} silently
 * keeps the last attribute written in its map, so a label declared by two attributes would feed one of them
 * with the other's data (for instance {@code POWERSTANDBY} for both standby attributes of the TV).
 */
class AttributeCatalogCollisionTest {

    /**
     * Collisions found by this test but out of the scope of GOU-1897, reported separately. The list may only
     * shrink: any new collision fails the build.
     */
    private static final Set<String> KNOWN_COLLISIONS = Set.of(
            "oven: 'PUISSANCE (W)' (all) -> POWER_CONSUMPTION_TYPICAL and OVEN_POWER",
            "oven: 'COULEUR' (all) -> COLOR and COULEUR_EXTERIEURE");

    private static List<VerticalConfig> verticals() throws Exception {
        VerticalsConfigService service = new VerticalsConfigService(new SerialisationService(),
                mock(GoogleTaxonomyService.class), new PathMatchingResourcePatternResolver());
        return new ArrayList<>(service.getConfigsWithoutDefault());
    }

    @Test
    void aSynonymIsDeclaredByOneAttributePerVerticalAndDatasource() throws Exception {
        List<String> collisions = new ArrayList<>();
        for (VerticalConfig vertical : verticals()) {
            Map<String, String> owners = new HashMap<>();
            for (AttributeConfig attribute : vertical.getAttributesConfig().getConfigs()) {
                attribute.getSynonyms().forEach((source, labels) -> labels.forEach(label -> {
                    String previous = owners.putIfAbsent(source + "|" + label, attribute.getKey());
                    if (previous != null && !previous.equals(attribute.getKey())) {
                        collisions.add(vertical.getId() + ": '" + label + "' (" + source + ") -> " + previous + " and "
                                + attribute.getKey());
                    }
                }));
            }
        }
        assertThat(collisions).as("synonym collisions").isSubsetOf(KNOWN_COLLISIONS);
    }

    @Test
    void anEprelFeatureFeedsOneAttributePerVerticalAndProductGroup() throws Exception {
        List<String> collisions = new ArrayList<>();
        for (VerticalConfig vertical : verticals()) {
            Map<String, Set<String>> owners = new HashMap<>();
            for (AttributeConfig attribute : vertical.getAttributesConfig().getConfigs()) {
                if (attribute.getReferentials() == null) {
                    continue;
                }
                for (EprelFeatureReferential ref : attribute.getReferentials().getEprel()) {
                    if (ref.getFeatureName() == null) {
                        continue;
                    }
                    owners.computeIfAbsent(ref.getProductGroup() + "|" + ref.getFeatureName(), k -> new TreeSet<>())
                            .add(attribute.getKey());
                }
            }
            owners.forEach((feature, keys) -> {
                if (keys.size() > 1) {
                    collisions.add(vertical.getId() + ": " + feature + " -> " + keys);
                }
            });
        }
        assertThat(collisions).as("EPREL feature collisions").isEmpty();
    }

    @Test
    void tvStandbyAttributesDoNotShareAnySourceLabel() throws Exception {
        VerticalConfig tv = verticals().stream().filter(v -> "tv".equals(v.getId())).findFirst().orElseThrow();
        AttributeConfig standby = byKey(tv, "POWER_CONSUMPTION_STANDBY");
        AttributeConfig networked = byKey(tv, "POWER_CONSUMPTION_STANDBY_NETWORKD");

        assertThat(standby.getSynonyms().getOrDefault("all", Set.of()))
                .doesNotContainAnyElementsOf(networked.getSynonyms().getOrDefault("all", Set.of()));
        assertThat(standby.eprelFeatureNames()).doesNotContainAnyElementsOf(networked.eprelFeatureNames());
    }

    private static AttributeConfig byKey(VerticalConfig vertical, String key) {
        return vertical.getAttributesConfig().getConfigs().stream().filter(a -> key.equals(a.getKey())).findFirst()
                .orElseThrow(() -> new AssertionError(key + " missing from " + vertical.getId()));
    }
}
