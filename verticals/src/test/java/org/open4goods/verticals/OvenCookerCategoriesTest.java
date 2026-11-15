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

    /** The lookup is an exact (datasource config name, category) match, no normalisation: each Fnac format has its own entry. */
    @Test
    void fnacCategoriesClassifyAsOven() {
        String prefixed = "362 | COOKING | " + FNAC_CATEGORY;
        assertThat(classify("Fnac FR", FNAC_CATEGORY)).isEqualTo("oven");
        assertThat(classify("Fnac FR", prefixed)).isEqualTo("oven");
        assertThat(classify("fnac.com", FNAC_CATEGORY)).isEqualTo("oven");
        assertThat(classify("fnac.com", prefixed)).isNull();
    }

    @Test
    void cookerCategoriesDoNotAttractHobsOrHoods() {
        assertThat(classify("darty.com", "GROS ELECTROMENAGER / TABLE DE CUISSON / PLAQUE DE CUISSON | "
                + "GROS ELECTROMENAGER>TABLE DE CUISSON>PLAQUE DE CUISSON | TABLE DE CUISSON | PLAQUE DE CUISSON"))
                        .isNotEqualTo("oven");
        assertThat(classify("Cdiscount FR", "ELECTROMENAGER | ELECTROMENAGER > GROS APPAREIL DE CUISSON > "
                + "HOTTE | GROS APPAREIL DE CUISSON | HOTTE")).isNotEqualTo("oven");
    }

    /** PEM (small appliances: kettles, coffee makers...) is too broad for ovens (GOU-2255). */
    @Test
    void fnacPemCategoriesDoNotClassifyAsOven() {
        assertThat(classify("Fnac FR", "362 | COOKING | PETIT MENAGER & CUISINE - CUISSON PEM")).isNull();
        assertThat(classify("Fnac FR", "PETIT MENAGER & CUISINE - CUISSON PEM")).isNull();
        assertThat(classify("fnac.com", "PETIT MENAGER & CUISINE CUISSON PEM")).isNull();
        assertThat(classify("rakuten.com-csv.yml", "ELECTROMENAGER | PETIT-APPAREIL-CUISSON")).isNull();
    }

    @Test
    void fnacGemOvenStaysOven() {
        assertThat(classify("Fnac FR", "PETIT MENAGER & CUISINE - CUISSON GEM")).isEqualTo("oven");
    }

    @Test
    void fnacGemHoodsAndHobsAreExcluded() {
        Map<String, String> hood = new LinkedHashMap<>();
        hood.put("Fnac FR", "PETIT MENAGER & CUISINE - CUISSON GEM");
        hood.put("Darty FR", "GROS ELECTROMENAGER / HOTTE / HOTTE CASQUETTE | GROS ELECTROMENAGER>HOTTE>HOTTE CASQUETTE");
        assertThat(service.getVerticalForCategories(hood)).isNull();

        Map<String, String> hob = new LinkedHashMap<>();
        hob.put("Fnac FR", "PETIT MENAGER & CUISINE - CUISSON GEM");
        hob.put("Cdiscount FR", "ELECTROMENAGER | ELECTROMENAGER > GROS APPAREIL DE CUISSON > TABLE DE CUISSON INDUCTION"
                + " - PLAQUE DE CUISSON INDUCTION | TABLE DE CUISSON");
        assertThat(service.getVerticalForCategories(hob)).isNull();
        // EPREL-confirmed ovens stay protected (GOU-1700)
        assertThat(service.getVerticalForCategories(hood, "ovens")).isNotNull();
    }

    @Test
    void dartyBuiltInOvenStaysOven() {
        assertThat(classify("Darty FR", "GROS ELECTROMENAGER / FOUR / FOUR ENCASTRABLE | "
                + "GROS ELECTROMENAGER>FOUR>FOUR ENCASTRABLE | FOUR | FOUR ENCASTRABLE")).isEqualTo("oven");
    }
}
