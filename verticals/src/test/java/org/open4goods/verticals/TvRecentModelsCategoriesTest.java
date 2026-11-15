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
 * Recent televisions (SAMSUNG TQ65S95H) were left without vertical because the
 * merchant categories are matched under the datasource names reported by the
 * feeds (GOU-2031).
 */
class TvRecentModelsCategoriesTest {

    private static VerticalsConfigService service;

    @BeforeAll
    static void setUp() throws Exception {
        service = new VerticalsConfigService(new SerialisationService(), mock(GoogleTaxonomyService.class),
                new PathMatchingResourcePatternResolver());
    }

    private static String classify(Map<String, String> categories) {
        VerticalConfig vertical = service.getVerticalForCategories(categories);
        return vertical == null ? null : vertical.getId();
    }

    @Test
    void samsungTq65s95hCategoriesClassifyAsTv() {
        Map<String, String> categories = new LinkedHashMap<>();
        categories.put("ManoMano FR", "ELECTROMENAGER > TV, IMAGE, SON > TELEVISION > TV OLED");
        categories.put("Darty FR",
                "TV, VIDEO, HOME CINEMA / TELEVISEUR / TV OLED | TV, VIDEO, HOME CINEMA / TELEVISEUR / TV OLED");
        categories.put("Villatech", "TV - IMAGE & SON/TELEVISEUR/TV OLED");
        categories.put("Fnac FR", "9 | TELEVISIONS | TV & VIDEO & HOME CINEMA TELEVISEUR | "
                + "APPAREILS ELECTRONIQUES > VIDEO > TELEVISEURS | APPAREILS ELECTRONIQUES > VIDEO > TELEVISEURS");
        categories.put("Cdiscount FR",
                "TV - VIDEO - SON | TV - VIDEO - SON > TELEVISEUR > TELEVISEUR LED | TELEVISEUR | TELEVISEUR LED");
        categories.put("Carrefour", "IMAGE ET SON > TELEVISEURS");
        categories.put("RueDuCommerce FR", "TV");
        categories.put("MDA Electromenager", "TV OLED");
        assertThat(classify(categories)).isEqualTo("tv");
    }

    @Test
    void eachMerchantCategoryAloneClassifiesAsTv() {
        assertThat(classify(Map.of("Carrefour", "IMAGE ET SON > TELEVISEURS"))).isEqualTo("tv");
        assertThat(classify(Map.of("Villatech", "TV - IMAGE & SON/TELEVISEUR/TV LED"))).isEqualTo("tv");
        assertThat(classify(Map.of("MDA Electromenager", "TV LED"))).isEqualTo("tv");
        assertThat(classify(Map.of("RueDuCommerce FR", "TV"))).isEqualTo("tv");
    }

    @Test
    void accessoriesStayOutOfTv() {
        assertThat(classify(Map.of("Carrefour", "IMAGE ET SON > ACCESSOIRES TV, VIDEO ET MEUBLES > SUPPORTS MURAUX TV")))
                .isNull();
        assertThat(classify(Map.of("Carrefour",
                "IMAGE ET SON > ACCESSOIRES TV, VIDEO ET MEUBLES > TELECOMMANDE"))).isNull();
        assertThat(classify(Map.of("Darty FR", "TV, VIDEO, HOME CINEMA / TELEVISEUR / SUPPORT MURAL POUR ECRAN PLAT | "
                + "TV, VIDEO, HOME CINEMA>TELEVISEUR>SUPPORT MURAL POUR ECRAN PLAT | TELEVISEUR | "
                + "SUPPORT MURAL POUR ECRAN PLAT"))).isNull();
        assertThat(classify(Map.of("Fnac FR", "9 | TELEVISIONS | TV & VIDEO & HOME CINEMA MEUBLE TV ET SUPPORT MURAL | "
                + "AMEUBLEMENT > MEUBLES DE SALON > MEUBLES AUDIO/VIDEO ET POUR HOME-CINEMA | "
                + "AMEUBLEMENT > MEUBLES DE SALON > MEUBLES AUDIO/VIDEO ET POUR HOME-CINEMA"))).isNull();
    }
}
