package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.model.product.ProductNature;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** Beds are not accessories because Fnac files them under an accessory category (GOU-2431). */
class BedNatureTest {

    private static final String FNAC = "451 | BEDS | PETIT MENAGER & CUISINE - ACCESSOIRES LITERIE | MAISON ET JARDIN > LINGE > LITERIE | MAISON ET JARDIN > LINGE > LITERIE";
    private static final String DARTY = "LITERIE / ACCESSOIRES LITERIE / CONTOUR CADRE DE LIT | ACCESSOIRES LITERIE | CONTOUR CADRE DE LIT";

    private final VerticalsConfigService service = new VerticalsConfigService(new SerialisationService(),
            mock(GoogleTaxonomyService.class), new PathMatchingResourcePatternResolver());

    @Test
    void fnacBedCategoryIsNotAnAccessoryMarker() {
        assertThat(service.getNatureForCategories(List.of(FNAC), 0d)).isNull();
        assertThat(service.getNatureForCategories(List.of("PETIT MENAGER & CUISINE - ACCESSOIRES LITERIE"), 0d)).isNull();
    }

    @Test
    void bedWithFnacAndDartyRowsOutOfFiveIsNotAccessory() {
        List<String> cats = List.of("MAISON ET DECORATION > CHAMBRE > LITS", FNAC, "LIT ADULTE",
                "MEUBLE > MEUBLE DE CHAMBRE > STRUCTURE DE LIT", DARTY);
        // Fnac row no longer marked: 1 of 5
        assertThat(service.getNatureForCategories(cats)).isNull();
    }

    @Test
    void realBeddingAccessoriesOfOtherMerchantsKeepTheirNature() {
        assertThat(service.getNatureForCategories(List.of("LITERIE > ACCESSOIRES LITERIE > PIED DE LIT")))
                .isEqualTo(ProductNature.ACCESSORY);
        assertThat(service.getNatureForCategories(List.of("LITERIE > ACCESSOIRES LITERIE > PROTEGE MATELAS")))
                .isEqualTo(ProductNature.ACCESSORY);
    }
}
