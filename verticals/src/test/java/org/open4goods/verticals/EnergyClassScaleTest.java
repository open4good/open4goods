package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * GOU-1897 point 1: the pre-2021 labels (A+++, A++, A+) used to score 100, like the A of the current scale.
 * They now carry no score, and the A to G labels stay strictly ordered.
 */
class EnergyClassScaleTest {

    private static final List<String> SCALE = List.of("A", "B", "C", "D", "E", "F", "G");

    @ParameterizedTest
    @ValueSource(strings = { "CLASSE_ENERGY", "CLASSE_ENERGY_SDR", "CLASSE_ENERGY_HDR" })
    void legacyLabelsAreNotScoredAndCurrentLabelsAreStrictlyOrdered(String key) throws Exception {
        VerticalsConfigService service = new VerticalsConfigService(new SerialisationService(),
                mock(GoogleTaxonomyService.class), new PathMatchingResourcePatternResolver());
        VerticalConfig tv = service.getConfigsWithoutDefault().stream().filter(v -> "tv".equals(v.getId()))
                .findFirst().orElseThrow();
        AttributeConfig attribute = tv.getAttributesConfig().getConfigs().stream()
                .filter(a -> key.equals(a.getKey())).findFirst().orElseThrow();

        assertThat(attribute.getNumericMapping()).containsOnlyKeys(SCALE);
        double previous = Double.MAX_VALUE;
        for (String label : SCALE) {
            double score = attribute.getNumericMapping().get(label);
            assertThat(score).as("%s must score below the previous class", label).isLessThan(previous);
            previous = score;
        }
        assertThat(attribute.getParser().getTokenMatch() == null
                || attribute.getParser().getTokenMatch().indexOf("A+++") < attribute.getParser().getTokenMatch().indexOf("A"))
                .as("A+++ must be matched before A so that it is never read as an A").isTrue();
    }
}
