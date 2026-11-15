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
 * They now carry no score, and the A to G labels stay strictly ordered. GOU-2137: air conditioners keep their own
 * A+++ to D scale (Delegated Regulation (EU) 626/2011), through a vertical override of {@code CLASSE_ENERGY}.
 */
class EnergyClassScaleTest {

    private static final List<String> SCALE = List.of("A", "B", "C", "D", "E", "F", "G");

    private static final List<String> AIR_CONDITIONER_SCALE = List.of("A+++", "A++", "A+", "A", "B", "C", "D");

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

    @Test
    void airConditionersScoreEveryClassOfTheirScaleInOrder() throws Exception {
        VerticalsConfigService service = new VerticalsConfigService(new SerialisationService(),
                mock(GoogleTaxonomyService.class), new PathMatchingResourcePatternResolver());
        VerticalConfig airConditioner = service.getConfigById("air-conditioner");
        AttributeConfig attribute = airConditioner.getAttributesConfig().getAttributeConfigByKey("CLASSE_ENERGY");

        // A+++, A++, A+ and A are the classes of the rated air conditioners in production (13/11/2026): none is imputed
        assertThat(attribute.getNumericMapping()).containsOnlyKeys(AIR_CONDITIONER_SCALE);
        double previous = Double.MAX_VALUE;
        for (String label : AIR_CONDITIONER_SCALE) {
            double score = attribute.getNumericMapping().get(label);
            assertThat(score).as("%s must score below the previous class", label).isLessThan(previous);
            previous = score;
        }
        assertThat(attribute.getNumericMapping()).containsEntry("A+++", attribute.getScoring().getNormalization()
                .getParams().getFixedMax()).containsEntry("D", attribute.getScoring().getNormalization().getParams()
                .getFixedMin());
        assertThat(attribute.isAsScore()).isTrue();

        AttributeConfig tvClass = service.getConfigById("tv").getAttributesConfig().getAttributeConfigByKey("CLASSE_ENERGY");
        assertThat(tvClass.getNumericMapping()).as("the catalogue scale is unchanged").containsOnlyKeys(SCALE);
        assertThat(attribute.getSynonyms()).isEqualTo(tvClass.getSynonyms());
        assertThat(attribute.getParser().getTokenMatch()).isEqualTo(tvClass.getParser().getTokenMatch());
        assertThat(attribute.getMappings()).isEqualTo(tvClass.getMappings());
    }
}
