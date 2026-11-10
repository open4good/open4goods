package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * GOU-1891: the EPREL keys of the smartphones and tablets group (Reg. (EU) 2023/1669) resolve to the intended
 * attributes of the smartphones vertical, and the EU repairability scale is never mixed with the French index.
 */
class SmartphoneEprelMappingTest {

    private static VerticalConfig smartphones() throws Exception {
        VerticalsConfigService service = new VerticalsConfigService(new SerialisationService(),
                mock(GoogleTaxonomyService.class), new PathMatchingResourcePatternResolver());
        return service.getConfigsWithoutDefault().stream().filter(v -> "smartphones".equals(v.getId())).findFirst()
                .orElseThrow();
    }

    @Test
    void eprelKeysOfTheGroupResolveToTheIntendedAttributes() throws Exception {
        Map<String, String> eprel = smartphones().getAttributesConfig().synonyms().get("eprel");
        assertThat(eprel).isNotNull()
                .containsEntry("REPAIRABILITYCLASS", "EU_REPAIRABILITY_CLASS")
                .containsEntry("REPAIRABILITYINDEX", "EU_REPAIRABILITY_INDEX")
                .containsEntry("REPEATEDFREEFALLRELIABILITYCLASS", "FREE_FALL_RELIABILITY_CLASS")
                .containsEntry("BATTERYENDURANCECYCLES", "BATTERY_ENDURANCE_CYCLES")
                .containsEntry("BATTERYENDURANCEPERCYCLEINHOURS", "BATTERY_ENDURANCE_PER_CYCLE")
                .containsEntry("INGRESSPROTECTIONRATING", "IP_RATING")
                .containsEntry("BATTERYUSERREPLACEABLE", "BATTERY_USER_REPLACEABLE")
                .containsEntry("MINYEARSSOFTWAREUPDATES", "MINAVAILABILITYSOFTWAREUPDATESYEARS")
                .containsEntry("OPERATINGSYSTEM", "OPERATING_SYSTEM")
                // raw keys with a trap stay unmapped: cycles in hundreds, durations in minutes
                .doesNotContainKey("BATTERYENDURANCEINCYCLES")
                .doesNotContainKey("BATTERYENDURANCEPERCYCLE");
    }

    @Test
    void euRepairabilityIsNeverMappedToTheFrenchIndex() throws Exception {
        VerticalConfig vertical = smartphones();
        AttributeConfig french = vertical.getAttributesConfig().getAttributeConfigByKey("REPAIRABILITY_INDEX");
        assertThat(french).isNotNull();
        french.getSynonyms().values().forEach(labels -> assertThat(labels).doesNotContain("REPAIRABILITYINDEX",
                "REPAIRABILITYCLASS"));
    }

    @Test
    void classesUseAFixedStrictlyOrderedScale() throws Exception {
        for (String key : new String[] { "EU_REPAIRABILITY_CLASS", "FREE_FALL_RELIABILITY_CLASS" }) {
            AttributeConfig attribute = smartphones().getAttributesConfig().getAttributeConfigByKey(key);
            assertThat(attribute.getScoring().getNormalization().getMethod().name()).isEqualTo("MINMAX_FIXED");
            assertThat(attribute.getNumericMapping()).containsOnlyKeys("A", "B", "C", "D", "E");
            double previous = Double.MAX_VALUE;
            for (String label : new String[] { "A", "B", "C", "D", "E" }) {
                double score = attribute.getNumericMapping().get(label);
                assertThat(score).isLessThan(previous);
                previous = score;
            }
        }
    }
}
