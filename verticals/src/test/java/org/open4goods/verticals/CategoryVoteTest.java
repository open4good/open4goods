package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Vertical resolution by a vote between datasources (GOU-2256), against the real vertical configurations.
 *
 * <p>Fixtures use the GTINs of the card. Their categories are <b>reconstructed</b>, not copied from Elasticsearch:
 * the public API does not expose merchant categories. The rows that give the wrong vertical are real
 * {@code matchingCategories} entries of the vertical cited by the card; the PC component rows use the three forms
 * the card quotes ({@code ... > COMPOSANT - PIECE DETACHEE > ...}, {@code BOITIER PC ...},
 * {@code INFORMATIQUE ET BUREAU > COMPOSANT PC ...}). Only the first form carries a marker
 * ({@code PIECE DETACHEE}, a spare part marker of {@code _default.yml}); the two others abstain. Each fixture names
 * the unknown that decides its result: the replay of the real rows is step 1 of the CTO measure.
 *
 * <p>Two service instances: {@code historical} keeps {@code _default.yml} as shipped (flag off), {@code vote} has
 * its own default configuration with the flag switched on.
 */
class CategoryVoteTest {

    /** Smartphone rows ({@code matchingCategories.all} of smartphones). */
    private static final String CDISCOUNT_PHONE = "TELEPHONE PORTABLE";
    private static final String CDISCOUNT_SMARTPHONE = "TELEPHONIE ET MOBILITE / TELEPHONE MOBILE / SMARTPHONE | "
            + "TELEPHONIE ET MOBILITE>TELEPHONE MOBILE>SMARTPHONE | TELEPHONE MOBILE | SMARTPHONE";
    private static final String FNAC_MOBILE_PHONES = "348 | MOBILE PHONES | SYSTEME PC & APPAREIL PORTABLE > "
            + "APPAREIL MOBILE > TELEPHONE MOBILE";
    private static final String LECLERC_SMARTPHONE = "HIGH-TECH | HIGH-TECH>SMARTPHONE - TELEPHONIE>SMARTPHONE>"
            + "SMARTPHONE ANDROID | SMARTPHONE - TELEPHONIE | SMARTPHONE";

    /** Rows matched per datasource. */
    private static final String RAKUTEN_DISHWASHER = "ELECTROMENAGER | LAVE-VAISSELLE";
    private static final String CDISCOUNT_DISHWASHER = "ELECTROMENAGER | ELECTROMENAGER > LAVAGE-SECHAGE > "
            + "LAVE-VAISSELLE | LAVAGE-SECHAGE | LAVE-VAISSELLE";
    private static final String RAKUTEN_REFRIGERATOR = "ELECTROMENAGER | REFRIGERATEUR";
    private static final String FNAC_REFRIGERATOR = "PETIT MENAGER & CUISINE - FROID";
    private static final String DARTY_REFRIGERATOR = "GROS ELECTROMENAGER / REFRIGERATEUR / REFRIGERATEUR 1 PORTE | "
            + "GROS ELECTROMENAGER / REFRIGERATEUR / REFRIGERATEUR 1 PORTE";
    private static final String RAKUTEN_TV = "HIFI | TELEVISION";
    private static final String DARTY_TV_OLED = "TV, VIDEO, HOME CINEMA / TELEVISEUR / TV OLED | TELEVISEUR | TV OLED";
    private static final String BOULANGER_TV = "TV - IMAGE - SON | TELEVISEUR";
    private static final String CDISCOUNT_TV_LED = "TV - VIDEO - SON | TV - VIDEO - SON > TELEVISEUR > TELEVISEUR LED | "
            + "TELEVISEUR | TELEVISEUR LED";
    private static final String LECLERC_TV_HISENSE = "HIGH-TECH | HIGH-TECH>TV - CINEMA>TELEVISEURS - TELEVISIONS - TV>"
            + "TV HISENSE | TV - CINEMA | TELEVISEURS - TELEVISIONS - TV";
    private static final String RAKUTEN_WASHING_MACHINE = "ELECTROMENAGER | LAVE-LINGE";
    private static final String DARTY_WASHING_MACHINE = "GROS ELECTROMENAGER | GROS ELECTROMENAGER>LAVE-LINGE>"
            + "LAVE-LINGE HUBLOT | LAVE-LINGE | GROS ELECTROMENAGER";

    /** Rows carrying a marker. */
    private static final String DARTY_SPARE_PART = "PIECES DETACHEES LAVE-LINGE > BAC PRODUIT LAVE LINGE";
    private static final String CDISCOUNT_WASHING_ACCESSORY = "ELECTROMENAGER > ACCESSOIRES POUR LAVE-LINGE ET SECHE-LINGE";
    private static final String AOSOM_SHOE_CARE = "MAISON ET JARDIN > PRODUITS MENAGERS > ENTRETIEN ET ACCESSOIRES "
            + "POUR CHAUSSURES > KITS D'ENTRETIEN DE CHAUSSURES";

    private static VerticalsConfigService historical;
    private static VerticalsConfigService vote;

    @BeforeAll
    static void setUp() throws Exception {
        historical = new VerticalsConfigService(new SerialisationService(), mock(GoogleTaxonomyService.class),
                new PathMatchingResourcePatternResolver());
        vote = new VerticalsConfigService(new SerialisationService(), mock(GoogleTaxonomyService.class),
                new PathMatchingResourcePatternResolver());
        vote.getDefaultConfig().setCategoryVoteEnabled(true);
    }

    /** A PC component row in the marked form of the card: votes against every vertical. */
    private static String markedComponent(String leaf) {
        return "INFORMATIQUE > COMPOSANT - PIECE DETACHEE > " + leaf;
    }

    /** A case: the categories, the result of the historical rule and the result of the vote. */
    private record Case(String name, Map<String, String> categories, String historicalVertical, String votedVertical) {
    }

    private static Map<String, String> categories(String... datasourceAndCategory) {
        Map<String, String> categories = new LinkedHashMap<>();
        for (int i = 0; i < datasourceAndCategory.length; i += 2) {
            categories.put(datasourceAndCategory[i], datasourceAndCategory[i + 1]);
        }
        return categories;
    }

    private static String classify(VerticalsConfigService service, Map<String, String> categories, String eprelGroup) {
        VerticalConfig vertical = service.getVerticalForCategories(categories, eprelGroup);
        return vertical == null ? null : vertical.getId();
    }

    /**
     * M.RED MR-036 PC case (3700755601091), in smartphones because Cdiscount FR and Fnac FR file it as a phone while
     * four other sources say PC case. Unknown that decides: how many of the four rows are in the marked form. The
     * fixture assumes two; with one, see {@link #unmarkedComponentRowsAbstain()}.
     */
    private static Map<String, String> mredCase() {
        return categories(
                "Cdiscount FR", CDISCOUNT_PHONE,
                "Fnac FR", FNAC_MOBILE_PHONES,
                "rakuten.com-csv.yml", markedComponent("BOITIER PC"),
                "alternate FR", markedComponent("BOITIER PC"),
                "grosbill.com", "BOITIER PC",
                "E.Leclerc FR", "INFORMATIQUE ET BUREAU > COMPOSANT PC > BOITIER PC");
    }

    /**
     * The false positives of the card: each holds one row matching the cited vertical and at least as many rows in
     * the marked form. Unknown that decides: that number of marked rows (the brief places the RAM, the PC cases and
     * the watercooling kits among the products of the marked form).
     */
    private static List<Case> falsePositives() {
        return List.of(
                new Case("M.RED MR-036 3700755601091", mredCase(), "smartphones", null),
                new Case("G.Skill RAM 4713294230263", categories(
                        "rakuten.com-csv.yml", RAKUTEN_DISHWASHER,
                        "alternate FR", markedComponent("MEMOIRE PC"),
                        "grosbill.com", "INFORMATIQUE ET BUREAU > COMPOSANT PC > MEMOIRE PC"), "dishwasher", null),
                new Case("Intel Arc A750 4710483940316", categories(
                        "Cdiscount FR", CDISCOUNT_DISHWASHER,
                        "rakuten.com-csv.yml", markedComponent("CARTE GRAPHIQUE")), "dishwasher", null),
                new Case("NZXT Kraken", categories(
                        "Joom FR", "SMARTPHONES",
                        "rakuten.com-csv.yml", markedComponent("WATERCOOLING")), "smartphones", null),
                new Case("ASRock DeskMeet 4710483944697", categories(
                        "rakuten.com-csv.yml", RAKUTEN_REFRIGERATOR,
                        "alternate FR", markedComponent("BAREBONE")), "refrigerator", null),
                new Case("Zalman P10", categories(
                        "Fnac FR", FNAC_REFRIGERATOR,
                        "rakuten.com-csv.yml", markedComponent("BOITIER PC"),
                        "grosbill.com", "BOITIER PC"), "refrigerator", null),
                new Case("Lian-Li HydroShift 4718466015754", categories(
                        "rakuten.com-csv.yml", RAKUTEN_TV,
                        "alternate FR", markedComponent("WATERCOOLING")), "tv", null));
    }

    /**
     * The real products of the card with one wrong "composant" row. Unknown that decides: at least two of their
     * rows match their vertical (no EPREL data on these products).
     */
    private static List<Case> realProducts() {
        return List.of(
                new Case("Xiaomi 14T 6941812790625", categories(
                        "Cdiscount FR", CDISCOUNT_SMARTPHONE,
                        "Fnac FR", FNAC_MOBILE_PHONES,
                        "E.Leclerc FR", LECLERC_SMARTPHONE,
                        "rakuten.com-csv.yml", markedComponent("PROCESSEUR")), "smartphones", "smartphones"),
                new Case("Huawei Nova 13 Pro 6942103145452", categories(
                        "Cdiscount FR", CDISCOUNT_SMARTPHONE,
                        "Joom FR", "SMARTPHONES",
                        "rakuten.com-csv.yml", markedComponent("CARTE MERE")), "smartphones", "smartphones"),
                new Case("Samsung OLED TQ83S95F 8806097077350", categories(
                        "Darty FR", DARTY_TV_OLED,
                        "Boulanger - Référentiel produits", BOULANGER_TV,
                        "rakuten.com-csv.yml", markedComponent("CARTE MERE")), "tv", "tv"),
                new Case("Hisense 50E77NQ 6942351402390", categories(
                        "Cdiscount FR", CDISCOUNT_TV_LED,
                        "E.Leclerc FR", LECLERC_TV_HISENSE,
                        "rakuten.com-csv.yml", markedComponent("PROCESSEUR")), "tv", "tv"));
    }

    /** Other cases whose result changes or must not change with the vote. */
    private static List<Case> otherCases() {
        return List.of(
                // SAMSUNG DC97-15234A detergent drawer (GOU-2236): one generic row against one spare part row
                new Case("spare part, one row each", categories(
                        "rakuten.com-csv.yml", RAKUTEN_WASHING_MACHINE,
                        "Darty FR", DARTY_SPARE_PART), null, null),
                // Old phones of one seller (GTIN 3663448...) under a plural spare part category: the excluding
                // token is a veto today, two phone rows outvote it
                new Case("phone with a spare part row", categories(
                        "Cdiscount FR", CDISCOUNT_SMARTPHONE,
                        "Fnac FR", FNAC_MOBILE_PHONES,
                        "rakuten.com-csv.yml", "PIECES DETACHEES TELEPHONE PORTABLE"), null, "smartphones"),
                // 24 washing machines measured under this accessory category (4e.0)
                new Case("washing machine with an accessory row", categories(
                        "darty.com", DARTY_WASHING_MACHINE,
                        "rakuten.com-csv.yml", RAKUTEN_WASHING_MACHINE,
                        "cdiscount.com", CDISCOUNT_WASHING_ACCESSORY), null, "washing-machine"),
                // SparePartsCategoriesTest#singularSparePartCategoryKeepsTheVertical: the singular form is a
                // spare part marker of 4e, so the basket row votes against: one against one
                new Case("dishwasher with a singular spare part row", categories(
                        "Cdiscount FR", CDISCOUNT_DISHWASHER,
                        "ManoMano FR", "ELECTROMENAGER > PIECE DETACHEE LAVE-VAISSELLE > PANIER"), "dishwasher", null),
                // Two verticals, one row each: the historical rule takes the first one in map order
                new Case("dishwasher and refrigerator, one row each", categories(
                        "Cdiscount FR", CDISCOUNT_DISHWASHER,
                        "Darty FR", DARTY_REFRIGERATOR), "dishwasher", null),
                new Case("two dishwasher rows and one refrigerator row", categories(
                        "Darty FR", DARTY_REFRIGERATOR,
                        "Cdiscount FR", CDISCOUNT_DISHWASHER,
                        "rakuten.com-csv.yml", RAKUTEN_DISHWASHER), "refrigerator", "dishwasher"),
                // Unmatched rows without marker and blank rows say nothing
                new Case("unmatched and blank rows abstain", categories(
                        "Cdiscount FR", CDISCOUNT_TV_LED,
                        "grosbill.com", "TV > TV 4K > 55 POUCES",
                        "Fnac FR", "HIGH-TECH",
                        "Joom FR", ""), "tv", "tv"),
                // The two air-conditioner matching categories holding QUINCAILLERIE: the marker wins over the
                // match, as the excluding token does today
                new Case("matching category holding an excluding token", categories(
                        "ManoMano FR", "428 | DIY | BRICOLAGE - OUTILLAGE - QUINCAILLERIE | BRICOLAGE - OUTILLAGE - "
                                + "QUINCAILLERIE > GENIE THERMIQUE - CLIMATIQUE - CHAUFFAGE > CLIMATISEUR MOBILE | "
                                + "GENIE THERMIQUE - CLIMATIQUE - CHAUFFAGE | CLIMATISEUR MOBILE"), null, null));
    }

    private static List<Case> allCases() {
        List<Case> cases = new ArrayList<>(falsePositives());
        cases.addAll(realProducts());
        cases.addAll(otherCases());
        return cases;
    }

    @Test
    void voteShipsDisabled() {
        assertThat(historical.getDefaultConfig().isCategoryVoteEnabled()).isFalse();
        assertThat(historical.isCategoryVoteEnabled()).isFalse();
        assertThat(vote.isCategoryVoteEnabled()).isTrue();
    }

    @Test
    void flagOffKeepsTheHistoricalResults() {
        for (Case c : allCases()) {
            assertThat(classify(historical, c.categories(), null)).as(c.name()).isEqualTo(c.historicalVertical());
        }
        // The historical rule depends on the map order
        Map<String, String> reversed = categories("Darty FR", DARTY_REFRIGERATOR, "Cdiscount FR", CDISCOUNT_DISHWASHER);
        assertThat(classify(historical, reversed, null)).isEqualTo("refrigerator");
    }

    @Test
    void falsePositivesLoseTheirVertical() {
        for (Case c : falsePositives()) {
            assertThat(classify(vote, c.categories(), null)).as(c.name()).isNull();
        }
    }

    @Test
    void realProductsWithOneComponentRowKeepTheirVertical() {
        for (Case c : realProducts()) {
            assertThat(classify(vote, c.categories(), null)).as(c.name()).isEqualTo(c.votedVertical());
        }
    }

    @Test
    void otherCasesFollowTheVote() {
        for (Case c : otherCases()) {
            assertThat(classify(vote, c.categories(), null)).as(c.name()).isEqualTo(c.votedVertical());
        }
    }

    /**
     * The limit of the rule: {@code BOITIER PC} and {@code ... COMPOSANT PC ...} carry no marker of
     * {@code _default.yml}, so they abstain. With a single marked row, M.RED keeps smartphones (2 for, 1 against).
     */
    @Test
    void unmarkedComponentRowsAbstain() {
        Map<String, String> oneMarkedRow = mredCase();
        oneMarkedRow.put("alternate FR", "BOITIER PC");

        assertThat(classify(vote, oneMarkedRow, null)).isEqualTo("smartphones");
    }

    @Test
    void resultDoesNotDependOnTheMapOrder() {
        for (Case c : allCases()) {
            for (Map<String, String> permutation : permutations(c.categories())) {
                assertThat(classify(vote, permutation, null)).as("%s, order %s", c.name(), permutation.keySet())
                        .isEqualTo(c.votedVertical());
            }
        }
    }

    /** A vertical confirmed by the EPREL group wins whatever the votes (GOU-1700). */
    @Test
    void eprelGroupKeepsItsVerticalWhateverTheVotes() {
        Map<String, String> outvoted = categories(
                "rakuten.com-csv.yml", RAKUTEN_WASHING_MACHINE,
                "Darty FR", DARTY_SPARE_PART,
                "aosom.fr", AOSOM_SHOE_CARE);

        assertThat(classify(vote, outvoted, null)).isNull();
        assertThat(classify(vote, outvoted, "washingmachines2019")).isEqualTo("washing-machine");
        assertThat(classify(vote, outvoted, "ovens")).as("EPREL group of another vertical").isNull();

        // Two verticals tied: the EPREL group decides
        Map<String, String> tied = categories("Cdiscount FR", CDISCOUNT_DISHWASHER, "Darty FR", DARTY_REFRIGERATOR);
        assertThat(classify(vote, tied, "refrigeratingappliances2019")).isEqualTo("refrigerator");
        assertThat(classify(vote, tied, "dishwashers2019")).isEqualTo("dishwasher");

        // A matching category holding a marker votes against its vertical, but still makes it a candidate
        Map<String, String> quincaillerie = otherCases().getLast().categories();
        assertThat(classify(vote, quincaillerie, null)).isNull();
        assertThat(classify(vote, quincaillerie, "airconditioners")).isEqualTo("air-conditioner");
    }

    /**
     * The EPREL group says washing machine, two rows say dishwasher: the vote gives no vertical, so the aggregation
     * keeps an EPREL-confirmed current vertical instead of moving the product (see
     * {@code TaxonomyRealTimeAggregationServiceCategoryVoteTest} in api).
     */
    @Test
    void eprelGroupOfAVerticalNoCategoryMatchesGivesNoVertical() {
        Map<String, String> dishwasherRows = categories(
                "Cdiscount FR", CDISCOUNT_DISHWASHER,
                "rakuten.com-csv.yml", RAKUTEN_DISHWASHER,
                "aosom.fr", AOSOM_SHOE_CARE);

        assertThat(classify(vote, dishwasherRows, null)).isEqualTo("dishwasher");
        assertThat(classify(vote, dishwasherRows, "washingmachines2019")).isNull();
        assertThat(classify(historical, dishwasherRows, "washingmachines2019"))
                .as("historical: the ACCESSOIRE token drops dishwasher").isNull();

        // Without the accessory row, the historical rule moves the product to dishwasher
        dishwasherRows.remove("aosom.fr");
        assertThat(classify(historical, dishwasherRows, "washingmachines2019")).isEqualTo("dishwasher");
        assertThat(classify(vote, dishwasherRows, "washingmachines2019")).isNull();
    }

    @Test
    void eprelGroupAloneNeverAssignsAVertical() {
        assertThat(classify(vote, categories("aosom.fr", "MAISON ET JARDIN > MEUBLES"), "washingmachines2019"))
                .isNull();
        assertThat(classify(vote, categories("Darty FR", DARTY_SPARE_PART), "washingmachines2019")).isNull();
    }

    @Test
    void emptyOrMissingCategoriesGiveNoVertical() {
        assertThat(vote.getVerticalByCategoryVote(null, null)).isNull();
        assertThat(vote.getVerticalByCategoryVote(Map.of(), "washingmachines2019")).isNull();
    }

    /**
     * The vote does not touch the nature of 4e: {@code COMPOSANT - PIECE DETACHEE} stays a neutral fragment there,
     * so a PC component left without vertical still gets no nature.
     */
    @Test
    void componentRowsVoteAgainstButStayNeutralForTheNature() {
        Map<String, String> ram = falsePositives().get(1).categories();

        assertThat(classify(vote, ram, null)).isNull();
        assertThat(vote.getNatureForCategories(ram.values())).isNull();
    }

    private static List<Map<String, String>> permutations(Map<String, String> categories) {
        List<Map<String, String>> permutations = new ArrayList<>();
        permute(new ArrayList<>(categories.entrySet()), 0, permutations);
        return permutations;
    }

    private static void permute(List<Map.Entry<String, String>> entries, int from,
            List<Map<String, String>> permutations) {
        if (from == entries.size()) {
            Map<String, String> permutation = new LinkedHashMap<>();
            entries.forEach(entry -> permutation.put(entry.getKey(), entry.getValue()));
            permutations.add(permutation);
            return;
        }
        for (int i = from; i < entries.size(); i++) {
            Collections.swap(entries, from, i);
            permute(entries, from + 1, permutations);
            Collections.swap(entries, from, i);
        }
    }
}
