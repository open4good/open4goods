package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Cookers with an electric oven carry the EPREL {@code ovens} label, so their
 * merchant categories classify into {@code oven} (GOU-1840).
 */
class OvenCookerCategoriesTest {

    private static final String FNAC_CATEGORY = "PETIT MENAGER & CUISINE CUISSON GEM";
    private static final String DARTY_CATEGORY = "GROS ELECTROMENAGER / CUISINIERES / PIANO DE CUISSON | "
            + "GROS ELECTROMENAGER>CUISINIERES>PIANO DE CUISSON | CUISINIERES | PIANO DE CUISSON";
    private static final String CDISCOUNT_CATEGORY = "ELECTROMENAGER | ELECTROMENAGER > GROS APPAREIL DE CUISSON > "
            + "CUISINIERE - PIANO DE CUISINE | GROS APPAREIL DE CUISSON | CUISINIERE - PIANO DE CUISINE";
    private static final String RAKUTEN_CATEGORY = "ELECTROMENAGER | CUISINIERE";

    private static VerticalsConfigService service;

    @BeforeAll
    static void setUp() throws Exception {
        service = new VerticalsConfigService(new SerialisationService(), mock(GoogleTaxonomyService.class),
                new PathMatchingResourcePatternResolver());
    }

    private static String classify(String datasource, String category) {
        Map<String, String> categories = new LinkedHashMap<>();
        categories.put(datasource, category);
        VerticalConfig vertical = service.getVerticalForCategories(categories, "ovens");
        return vertical == null ? null : vertical.getId();
    }

    @Test
    void cookerCategoriesClassifyAsOven() {
        assertThat(classify("darty.com", DARTY_CATEGORY)).isEqualTo("oven");
        assertThat(classify("Darty FR", DARTY_CATEGORY)).isEqualTo("oven");
        assertThat(classify("Cdiscount FR", CDISCOUNT_CATEGORY)).isEqualTo("oven");
        assertThat(classify("cdiscount.com", CDISCOUNT_CATEGORY)).isEqualTo("oven");
        assertThat(classify("rakuten.com-csv.yml", RAKUTEN_CATEGORY)).isEqualTo("oven");
    }

    /** The lookup is an exact (datasource config name, category) match: the Fnac entry only applies under its key. */
    @Test
    void fnacCategoryOnlyMatchesUnderItsConfiguredDatasourceKey() {
        assertThat(classify("Fnac FR", FNAC_CATEGORY)).isEqualTo("oven");
        assertThat(classify("fnac.com", FNAC_CATEGORY)).isNull();
    }

    @Test
    void cookerCategoriesDoNotAttractHobsOrHoods() {
        assertThat(classify("darty.com", "GROS ELECTROMENAGER / TABLE DE CUISSON / PLAQUE DE CUISSON | "
                + "GROS ELECTROMENAGER>TABLE DE CUISSON>PLAQUE DE CUISSON | TABLE DE CUISSON | PLAQUE DE CUISSON"))
                        .isNotEqualTo("oven");
        assertThat(classify("Cdiscount FR", "ELECTROMENAGER | ELECTROMENAGER > GROS APPAREIL DE CUISSON > "
                + "HOTTE | GROS APPAREIL DE CUISSON | HOTTE")).isNotEqualTo("oven");
    }
}
