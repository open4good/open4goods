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
 * Spare parts stay out of the verticals (GOU-2236): a merchant category naming "pièces détachées" is an
 * excluding token, as "accessoire" already is. Categories reach the matching upper-cased and without accents
 * ({@code IdHelper.getCategoryName}).
 */
class SparePartsCategoriesTest {

    /** Generic Rakuten category matched by the washing-machine vertical. */
    private static final String RAKUTEN_WASHING_MACHINE = "ELECTROMENAGER | LAVE-LINGE";

    /** Darty category of SAMSUNG DC97-15234A, a detergent drawer (ImpactScore audit sample, lot 1). */
    private static final String DARTY_SPARE_PART = "PIECES DETACHEES LAVE-LINGE > BAC PRODUIT LAVE LINGE";

    private static VerticalsConfigService service;

    @BeforeAll
    static void setUp() throws Exception {
        service = new VerticalsConfigService(new SerialisationService(), mock(GoogleTaxonomyService.class),
                new PathMatchingResourcePatternResolver());
    }

    private static String classify(Map<String, String> categories, String eprelGroup) {
        VerticalConfig vertical = service.getVerticalForCategories(categories, eprelGroup);
        return vertical == null ? null : vertical.getId();
    }

    private static Map<String, String> samsungDetergentDrawer() {
        Map<String, String> categories = new LinkedHashMap<>();
        categories.put("rakuten.com-csv.yml", RAKUTEN_WASHING_MACHINE);
        categories.put("Darty FR", DARTY_SPARE_PART);
        return categories;
    }

    @Test
    void genericCategoryAloneStillMatchesTheVertical() {
        assertThat(classify(Map.of("rakuten.com-csv.yml", RAKUTEN_WASHING_MACHINE), null))
                .isEqualTo("washing-machine");
    }

    @Test
    void sparePartCategoryOfAnotherMerchantRemovesTheVertical() {
        assertThat(classify(samsungDetergentDrawer(), null)).isNull();
    }

    @Test
    void singularSparePartCategoryVotesAgainstTheVertical() {
        // Merchants also file real appliances under the singular form (30 products measured, about ten of them
        // phones and TVs, GOU-2236): only the plural "PIECES DETACHEE" is an excluding token. Since the vote
        // (GOU-2317) the singular is one vote against, as a spare part marker of the nature: one row against one
        // loses. The historical rule keeps the vertical.
        Map<String, String> categories = new LinkedHashMap<>();
        categories.put("Cdiscount FR",
                "ELECTROMENAGER | ELECTROMENAGER > LAVAGE-SECHAGE > LAVE-VAISSELLE | LAVAGE-SECHAGE | LAVE-VAISSELLE");
        categories.put("ManoMano FR", "ELECTROMENAGER > PIECE DETACHEE LAVE-VAISSELLE > PANIER");
        assertThat(classify(categories, null)).isNull();
        service.getDefaultConfig().setCategoryVoteEnabled(false);
        try {
            assertThat(classify(categories, null)).isEqualTo("dishwasher");
        } finally {
            service.getDefaultConfig().setCategoryVoteEnabled(true);
        }
    }

    @Test
    void eprelGroupOfTheVerticalStillKeepsIt() {
        // An EPREL registration proves an appliance: one spare-part row cannot remove it (GOU-1700).
        assertThat(classify(samsungDetergentDrawer(), "washingmachines2019")).isEqualTo("washing-machine");
    }

    @Test
    void sparePartTokensAreInheritedByEveryVertical() {
        assertThat(service.getConfigsWithoutDefault())
                .isNotEmpty()
                .allSatisfy(vertical -> assertThat(vertical.getExcludingTokensFromCategoriesMatching())
                        .as("excluding tokens of %s", vertical.getId())
                        .contains("PIECES DETACHEE")
                        .doesNotContain("PIECE DETACHEE"));
    }

    @Test
    void noMatchingCategoryContainsAnExcludingToken() {
        // A matched category holding an excluding token could never assign its vertical.
        for (VerticalConfig vertical : service.getConfigsWithoutDefault()) {
            vertical.getMatchingCategories().values().stream()
                    .filter(categories -> categories != null)
                    .flatMap(categories -> categories.stream())
                    .forEach(category -> assertThat(category)
                            .as("matching category of %s", vertical.getId())
                            .doesNotContain("PIECES DETACHEE"));
        }
    }
}
