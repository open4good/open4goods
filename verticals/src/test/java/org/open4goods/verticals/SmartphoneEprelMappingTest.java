package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.model.eprel.EprelModelAlias;
import org.open4goods.model.eprel.EprelModelAliasTable;
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

    private static EprelModelAliasTable appleTable() throws Exception {
        List<EprelModelAliasTable> tables = smartphones().getEprelModelAliases();
        assertThat(tables).extracting(EprelModelAliasTable::getBrand).containsExactly("APPLE");
        return tables.getFirst();
    }

    /**
     * GOU-2042: every Apple smartphone registered in EPREL (eprel-products, productGroup
     * smartphonestablets20231669, deviceType SMARTPHONE, read on 2026-11-10) is reachable from the table.
     */
    @Test
    void appleTableCoversEveryAppleSmartphoneRegisteredInEprel() throws Exception {
        assertThat(appleTable().getModels()).extracting(EprelModelAlias::eprelModel).contains("A3090", "A3094",
                "A3287", "A3290", "A3293", "A3296", "A3409", "A3517", "A3520", "A3523", "A3526", "A3634", "A3714",
                "A3717", "A3720");
    }

    /**
     * GOU-2042: titles and part numbers of real catalogue products (products-moustik, 2026-11-10).
     */
    @Test
    void appleTableResolvesRealCatalogueProducts() throws Exception {
        EprelModelAliasTable apple = appleTable();
        // 195949040702: no regulatory number, made-up model
        assertThat(apple.resolveEprelModel("APPLE", List.of("Smartphone Apple Iphone 15 Plus 6,7\" A16 128 Gb Noir",
                "iPhone 15 Plus Noir 128 Go", "Apple iPhone 15 Plus 6Go/128Go - Noir", "IPHONE15PLUS-128-BLK")))
                .contains("A3094");
        // 195949041273: part number and merchant MPN only
        assertThat(apple.resolveEprelModel("APPLE", List.of("MU163ZD/A", "iPhone 15 Plus 128Go Bleu 5G", "iPhone")))
                .contains("A3094");
        // 195950639216
        assertThat(apple.resolveEprelModel("APPLE", List.of("Apple iPhone 17 Pro Max 6.9\" 256GB Bleu Foncé",
                "Apple Iphone 17 Pro Max 17,5 Cm (6.9\") Double Sim Ios 26 5g Usb Type-c 256 Go Bleu")))
                .contains("A3526");
        // 195949822612: one merchant title names the 16 Plus, the majority names the iPhone 16
        assertThat(apple.resolveEprelModel("APPLE", List.of("iPhone 16 Plus 128 Go - Sarcelle - Débloqué",
                "Apple iPhone 16 8Go/128Go - Turquoise", "Apple iPhone 16 - 128 Go - Turquoise",
                "Apple Iphone 16 6.1\" 128 Go Teal"))).contains("A3287");
        // iPhone 15 Pro has no EPREL record: it must never resolve to the iPhone 15 (A3090)
        assertThat(apple.resolveEprelModel("APPLE", List.of("Apple iPhone 15 Pro 128 Go Titane naturel")))
                .contains("A3102");
        // older models are out of the table
        assertThat(apple.resolveEprelModel("APPLE", List.of("Apple iPhone 11 6.1\" 256 Go Double SIM Blanc V2",
                "MHDQ3F/A"))).isEmpty();
    }
}
