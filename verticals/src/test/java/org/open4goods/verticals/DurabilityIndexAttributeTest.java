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
 * GOU-1892: the French durability index is a fixed 0-10 scale read from merchant attributes, in TV and washing machines.
 */
class DurabilityIndexAttributeTest {

    private VerticalConfig vertical(String id) throws Exception {
        VerticalsConfigService service = new VerticalsConfigService(new SerialisationService(),
                mock(GoogleTaxonomyService.class), new PathMatchingResourcePatternResolver());
        return service.getConfigsWithoutDefault().stream().filter(v -> id.equals(v.getId())).findFirst().orElseThrow();
    }

    @ParameterizedTest
    @ValueSource(strings = { "tv", "washing-machine" })
    void durabilityIndexIsAFixedZeroToTenScale(String verticalId) throws Exception {
        AttributeConfig attribute = vertical(verticalId).getAttributesConfig().getConfigs().stream()
                .filter(a -> "DURABILITY_INDEX".equals(a.getKey())).findFirst().orElseThrow();

        assertThat(attribute.getScoring().getNormalization().getMethod().name()).isEqualTo("MINMAX_FIXED");
        assertThat(attribute.getScoring().getNormalization().getParams().getFixedMin()).isEqualTo(0.0);
        assertThat(attribute.getScoring().getNormalization().getParams().getFixedMax()).isEqualTo(10.0);
        assertThat(attribute.getScoring().getScale().getMax()).isEqualTo(5.0);
        assertThat(attribute.getSynonyms().get("all")).contains("INDICE DE DURABILITÉ", "DURABILITY INDEX");
        assertThat(attribute.getParser().getDeleteTokens()).containsAll(List.of("/10", "SUR 10"));
    }
}
