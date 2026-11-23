package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Vertical resolution by a vote between datasources (GOU-2256), against the real vertical configurations.
 *
 * <p>The eleven products of the card are replayed on their <b>real</b> categories:
 * {@value #REAL_CATEGORIES} holds the {@code categoriesByDatasources} read by the CTO in {@code products-moustik} on
 * 16/11/2026 (document {@code fixtures} of GOU-2317), in their index order, and the EPREL group
 * {@code electronicdisplays} of the two TVs, in the export format of {@link CategoryVoteReplay}. The other cases are
 * built from real {@code matchingCategories} entries to pin each rule of the vote.
 *
 * <p>Two service instances: {@code historical} keeps {@code _default.yml} as shipped (flag off), {@code vote} has
 * its own default configuration with the flag switched on.
 */
class CategoryVoteTest {

    /** Real categories of the products of the card, export format of {@link CategoryVoteReplay}. */
    static final String REAL_CATEGORIES = "/category-vote/real-categories-2026-11-16.json";

    /** Real categories of the measure of 23/11/2026 (GOU-2317), export format of {@link CategoryVoteReplay}. */
    static final String EXCLUDING_TOKENS = "/category-vote/excluding-tokens-2026-11-23.json";

    /** Smartphone rows ({@code matchingCategories.all} of smartphones). */
    private static final String CDISCOUNT_SMARTPHONE = "TELEPHONIE ET MOBILITE / TELEPHONE MOBILE / SMARTPHONE | "
            + "TELEPHONIE ET MOBILITE>TELEPHONE MOBILE>SMARTPHONE | TELEPHONE MOBILE | SMARTPHONE";
    private static final String FNAC_MOBILE_PHONES = "348 | MOBILE PHONES | SYSTEME PC & APPAREIL PORTABLE > "
            + "APPAREIL MOBILE > TELEPHONE MOBILE";

    /** Rows matched per datasource. */
    private static final String RAKUTEN_DISHWASHER = "ELECTROMENAGER | LAVE-VAISSELLE";
    private static final String CDISCOUNT_DISHWASHER = "ELECTROMENAGER | ELECTROMENAGER > LAVAGE-SECHAGE > "
            + "LAVE-VAISSELLE | LAVAGE-SECHAGE | LAVE-VAISSELLE";
    private static final String DARTY_REFRIGERATOR = "GROS ELECTROMENAGER / REFRIGERATEUR / REFRIGERATEUR 1 PORTE | "
            + "GROS ELECTROMENAGER / REFRIGERATEUR / REFRIGERATEUR 1 PORTE";
    private static final String CDISCOUNT_TV_LED = "TV - VIDEO - SON | TV - VIDEO - SON > TELEVISEUR > TELEVISEUR LED | "
            + "TELEVISEUR | TELEVISEUR LED";
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
    private static Map<String, CategoryVoteReplay.ExportedProduct> realCategories;

    @BeforeAll
    static void setUp() throws Exception {
        historical = new VerticalsConfigService(new SerialisationService(), mock(GoogleTaxonomyService.class),
                new PathMatchingResourcePatternResolver());
        historical.getDefaultConfig().setCategoryVoteEnabled(false);
        vote = new VerticalsConfigService(new SerialisationService(), mock(GoogleTaxonomyService.class),
                new PathMatchingResourcePatternResolver());
        vote.getDefaultConfig().setCategoryVoteEnabled(true);
        realCategories = readRealCategories();
    }

    /**
     * @return the real categories of the card, by GTIN, in the order of the file
     * @throws IOException when the resource cannot be read
     */
    static Map<String, CategoryVoteReplay.ExportedProduct> readRealCategories() throws IOException {
        return readExport(REAL_CATEGORIES);
    }

    private static Map<String, CategoryVoteReplay.ExportedProduct> readExport(String resource) throws IOException {
        try (InputStream input = CategoryVoteTest.class.getResourceAsStream(resource)) {
            assertThat(input).as(resource).isNotNull();
            return CategoryVoteReplay.readExport(input);
        }
    }

    /**
     * A case: the categories, the EPREL group, the result of the historical rule and the result of the vote.
     */
    private record Case(String name, Map<String, String> categories, String eprelGroup, String historicalVertical,
            String votedVertical) {

        Case(String name, Map<String, String> categories, String historicalVertical, String votedVertical) {
            this(name, categories, null, historicalVertical, votedVertical);
        }
    }

    /** A product of the card, on its real categories and EPREL group. */
    private static Case real(String gtin, String name, String historicalVertical, String votedVertical) {
        CategoryVoteReplay.ExportedProduct product = realCategories.get(gtin);
        assertThat(product).as("%s %s in %s", name, gtin, REAL_CATEGORIES).isNotNull();
        return new Case(name + " " + gtin, new LinkedHashMap<>(product.categoriesByDatasources()),
                product.eprelGroup(), historicalVertical, votedVertical);
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
     * The false positives of the card: a few rows file them in a vertical, the PC component rows outvote them. The
     * historical results are those of the CTO replay. G.Skill already has no vertical with the historical rule (the
     * {@code PIECES DETACHEES} token of a Darty row, lot 4e.4). Lian-Li had two tv rows against the two
     * {@code COMPOSANT - PIECE DETACHEE} rows of {@code cdiscount.com} and {@code Cdiscount FR} (two voters), a tie;
     * its {@code INFORMATIQUE | COMPOSANTS | BOITIER} row now adds a vote against. Zalman needs the PC component
     * markers of GOU-2317 ({@code BOITIER PC}, {@code INFORMATIQUE | COMPOSANTS |}).
     */
    private static List<Case> falsePositives() {
        return List.of(
                real("3700755601091", "M.RED MR-036", "smartphones", null),
                real("4713294230263", "G.Skill RAM", null, null),
                real("4710483940316", "Intel Arc A750", "dishwasher", null),
                real("5056547202259", "NZXT Kraken", "smartphones", null),
                real("4710483944697", "ASRock DeskMeet", "refrigerator", null),
                real("4718466015754", "Lian-Li HydroShift", "tv", null),
                real("8800263650385", "Zalman P10", "refrigerator", null));
    }

    /**
     * The real products of the card, each with one wrong {@code COMPOSANT - PIECE DETACHEE} row. The two TVs carry the
     * EPREL group {@code electronicdisplays} of tv.
     */
    private static List<Case> realProducts() {
        return List.of(
                real("6941812790625", "Xiaomi 14T", "smartphones", "smartphones"),
                real("6942103145452", "Huawei Nova 13 Pro", "smartphones", "smartphones"),
                real("8806097077350", "Samsung OLED TQ83S95F", "tv", "tv"),
                real("6942351402390", "Hisense 50E77NQ", "tv", "tv"));
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

    /** Switched on after the before/after measure (GOU-2317). */
    @Test
    void voteShipsEnabled() {
        VerticalsConfigService shipped = new VerticalsConfigService(new SerialisationService(),
                mock(GoogleTaxonomyService.class), new PathMatchingResourcePatternResolver());
        assertThat(shipped.getDefaultConfig().isCategoryVoteEnabled()).isTrue();
        assertThat(shipped.isCategoryVoteEnabled()).isTrue();
        assertThat(historical.isCategoryVoteEnabled()).isFalse();
    }

    @Test
    void flagOffKeepsTheHistoricalResults() {
        for (Case c : allCases()) {
            assertThat(classify(historical, c.categories(), c.eprelGroup())).as(c.name())
                    .isEqualTo(c.historicalVertical());
        }
        // The historical rule depends on the map order
        Map<String, String> reversed = categories("Darty FR", DARTY_REFRIGERATOR, "Cdiscount FR", CDISCOUNT_DISHWASHER);
        assertThat(classify(historical, reversed, null)).isEqualTo("refrigerator");
    }

    @Test
    void falsePositivesLoseTheirVertical() {
        for (Case c : falsePositives()) {
            assertThat(classify(vote, c.categories(), c.eprelGroup())).as(c.name()).isNull();
        }
    }

    @Test
    void realProductsWithOneComponentRowKeepTheirVertical() {
        for (Case c : realProducts()) {
            assertThat(classify(vote, c.categories(), c.eprelGroup())).as(c.name()).isEqualTo(c.votedVertical());
        }
    }

    /** The real products keep their vertical on their votes alone: the EPREL group of the TVs is not what saves them. */
    @Test
    void realProductsKeepTheirVerticalWithoutEprel() {
        for (Case c : realProducts()) {
            assertThat(classify(vote, c.categories(), null)).as(c.name()).isEqualTo(c.votedVertical());
        }
    }

    @Test
    void otherCasesFollowTheVote() {
        for (Case c : otherCases()) {
            assertThat(classify(vote, c.categories(), c.eprelGroup())).as(c.name()).isEqualTo(c.votedVertical());
        }
    }

    /**
     * The known limit of GOU-2256, lifted by GOU-2317: the PC component categories without {@code PIECE DETACHEE}
     * ({@code BOITIER PC}, {@code ... COMPOSANT PC ...}, {@code ... COMPOSANTS PC ...},
     * {@code INFORMATIQUE | COMPOSANTS | ...}) now vote against. Zalman P10 had one refrigerator row for, and two such
     * rows that abstained: it kept refrigerator; it now loses it two against one.
     */
    @Test
    void pcComponentRowsVoteAgainst() {
        Map<String, String> zalman = falsePositives().getLast().categories();
        assertThat(zalman).hasSize(3);
        assertThat(classify(vote, zalman, null)).isNull();

        // Each form alone against one refrigerator row: one for, one against, the tie loses
        for (String component : List.of("BOITIER PC", "INFORMATIQUE ET BUREAU > COMPOSANT PC > BOITIER PC",
                "INFORMATIQUE / COMPOSANTS PC / CARTE GRAPHIQUE | INFORMATIQUE>COMPOSANTS PC>CARTE GRAPHIQUE | "
                        + "COMPOSANTS PC | CARTE GRAPHIQUE",
                "INFORMATIQUE | COMPOSANTS | BOITIER")) {
            Map<String, String> tie = categories("Cdiscount FR", zalman.get("Cdiscount FR"), "grosbill.com", component);
            assertThat(classify(vote, tie, null)).as(component).isNull();
            assertThat(classify(historical, tie, null)).as("historical, %s", component).isEqualTo("refrigerator");
        }
    }

    /**
     * A tie opposed only by another vertical goes to the historical rule (GOU-2317). Fnac files dishwashers under
     * {@code PETIT MENAGER & CUISINE - LAVAGE}, a washing machine category, and air conditioners under
     * {@code PETIT MENAGER & CUISINE - FROID}, a refrigerator one; the long {@code Darty FR} categories are only mapped
     * under {@code darty.com}. With "a tie loses", 26 of 1,412 air conditioners and about 27 of 1,497 dishwashers of
     * the measure lost their vertical. Real products of {@code products-moustik}, 23/11/2026.
     */
    @Test
    void tieBetweenVerticalsFollowsTheHistoricalRule() {
        Map<String, String> dishwasher = categories(
                "darty.com", "GROS ELECTROMENAGER / LAVE-VAISSELLE / LAVE-VAISSELLE ENCASTRABLE | GROS ELECTROMENAGER>"
                        + "LAVE-VAISSELLE>LAVE-VAISSELLE ENCASTRABLE | LAVE-VAISSELLE | LAVE-VAISSELLE ENCASTRABLE",
                "fnac.com", "PETIT MENAGER & CUISINE - LAVAGE",
                "Darty FR", "GROS ELECTROMENAGER / LAVE-VAISSELLE / LAVE-VAISSELLE ENCASTRABLE | GROS ELECTROMENAGER>"
                        + "LAVE-VAISSELLE>LAVE-VAISSELLE ENCASTRABLE | LAVE-VAISSELLE | LAVE-VAISSELLE ENCASTRABLE",
                "Fnac FR", "PETIT MENAGER & CUISINE - LAVAGE");
        assertThat(classify(vote, dishwasher, null)).as("dishwasher 3666749654699").isEqualTo("dishwasher");

        Map<String, String> airConditioner = categories(
                "RueDuCommerce FR", "CLIMATISEUR",
                "Fnac FR", "PETIT MENAGER & CUISINE - FROID",
                "Carrefour", "ENTRETIEN DE LA MAISON",
                "Darty FR", "GROS ELECTROMENAGER / CLIMATISEUR / CLIMATISEUR MOBILE | GROS ELECTROMENAGER>CLIMATISEUR>"
                        + "CLIMATISEUR MOBILE | CLIMATISEUR | CLIMATISEUR MOBILE");
        assertThat(classify(vote, airConditioner, null)).as("air conditioner 8436607027089")
                .isEqualTo("air-conditioner");

        // Two verticals, one row each: only this tie depends on the map order, as the historical rule does
        assertThat(classify(vote, categories("Cdiscount FR", CDISCOUNT_DISHWASHER, "Darty FR", DARTY_REFRIGERATOR),
                null)).isEqualTo("dishwasher");
        assertThat(classify(vote, categories("Darty FR", DARTY_REFRIGERATOR, "Cdiscount FR", CDISCOUNT_DISHWASHER),
                null)).isEqualTo("refrigerator");
    }

    /** A tie with a marker or an excluding token against the vertical still loses, whatever the map order. */
    @Test
    void tieWithAMarkerAgainstLoses() {
        Map<String, String> sparePart = categories("Cdiscount FR", CDISCOUNT_DISHWASHER,
                "ManoMano FR", "ELECTROMENAGER > PIECE DETACHEE LAVE-VAISSELLE > PANIER");
        for (Map<String, String> order : orders(sparePart)) {
            assertThat(classify(vote, order, null)).as("order %s", order.keySet()).isNull();
        }
    }

    /**
     * Excluding tokens of GOU-2317 on real products of the measure ({@value #EXCLUDING_TOKENS}). Three PC monitors
     * or signage displays that Cdiscount and Rakuten file as TVs (Philips 8712581774455, iiyama 4948570121823, Samsung
     * 8806094916119) and a power bank filed under Darty {@code TELEPHONE MOBILE} (8720249803386) gained a vertical by
     * the vote: they now get none, as with the historical rule. Three real TVs with a monitor-like row keep tv under
     * both rules: RueDuCommerce {@code ECRAN PC} (Fox Spirit 3601600010610), Cdiscount {@code ECRAN ORDINATEUR} (LG
     * hotel TV 8806087971811) and alternate {@code TV & MONITEUR > TV} (8806094859423).
     */
    @Test
    void excludingTokensOfTheMeasure() throws IOException {
        Map<String, CategoryVoteReplay.ExportedProduct> products = readExport(EXCLUDING_TOKENS);
        for (String gtin : List.of("8712581774455", "4948570121823", "8806094916119", "8720249803386")) {
            Map<String, String> categories = products.get(gtin).categoriesByDatasources();
            assertThat(classify(historical, categories, null)).as("historical %s", gtin).isNull();
            assertThat(classify(vote, categories, null)).as("vote %s", gtin).isNull();
        }
        for (String gtin : List.of("3601600010610", "8806087971811", "8806094859423")) {
            Map<String, String> categories = products.get(gtin).categoriesByDatasources();
            assertThat(classify(historical, categories, null)).as("historical %s", gtin).isEqualTo("tv");
            assertThat(classify(vote, categories, null)).as("vote %s", gtin).isEqualTo("tv");
        }
    }

    /** No generic {@code COMPOSANT} token: the PC component markers name the PC, or the Rakuten computing branch. */
    @Test
    void noGenericComponentMarker() {
        for (String marker : List.of("COMPOSANT", "COMPOSANTS", "COMPOSANT - ", "INFORMATIQUE")) {
            assertThat(vote.getDefaultConfig().getSparePartCategoryMarkers()).as(marker).doesNotContain(marker);
            assertThat(vote.getDefaultConfig().getAccessoryCategoryMarkers()).as(marker).doesNotContain(marker);
        }
        assertThat(vote.carriesNatureMarker("TV - VIDEO - SON > COMPOSANTS AUDIO")).isFalse();
    }

    /**
     * The vote decides on the bag of votes, not on the order of the map. Every order is tried up to six rows; beyond,
     * the real products have up to ten rows (3.6 M orders), so the identity, every rotation of it and of its reverse,
     * and 50 seeded shuffles.
     */
    @Test
    void resultDoesNotDependOnTheMapOrder() {
        for (Case c : allCases()) {
            for (Map<String, String> order : orders(c.categories())) {
                assertThat(classify(vote, order, c.eprelGroup())).as("%s, order %s", c.name(), order.keySet())
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
     * The vote does not touch the nature of 4e: {@code COMPOSANT - PIECE DETACHEE} and the PC component markers are
     * neutral fragments there, so a PC component left without vertical still gets no nature. Proof over every real
     * category: {@code PcComponentMarkersTest}.
     */
    @Test
    void componentRowsVoteAgainstButStayNeutralForTheNature() {
        for (Case c : List.of(falsePositives().get(2), falsePositives().getLast())) {
            assertThat(classify(vote, c.categories(), null)).as(c.name()).isNull();
            assertThat(vote.getNatureForCategories(c.categories().values())).as(c.name()).isNull();
        }
    }

    /** Every order up to six entries, a bounded sample beyond. */
    private static List<Map<String, String>> orders(Map<String, String> categories) {
        List<Map.Entry<String, String>> entries = new ArrayList<>(categories.entrySet());
        List<Map<String, String>> orders = new ArrayList<>();
        if (entries.size() <= 6) {
            permute(entries, 0, orders);
            return orders;
        }
        List<Map.Entry<String, String>> reversed = new ArrayList<>(entries);
        Collections.reverse(reversed);
        for (List<Map.Entry<String, String>> base : List.of(entries, reversed)) {
            for (int shift = 0; shift < base.size(); shift++) {
                List<Map.Entry<String, String>> rotation = new ArrayList<>(base);
                Collections.rotate(rotation, shift);
                orders.add(asMap(rotation));
            }
        }
        Random random = new Random(2317);
        for (int i = 0; i < 50; i++) {
            List<Map.Entry<String, String>> shuffled = new ArrayList<>(entries);
            Collections.shuffle(shuffled, random);
            orders.add(asMap(shuffled));
        }
        return orders;
    }

    private static Map<String, String> asMap(List<Map.Entry<String, String>> entries) {
        Map<String, String> map = new LinkedHashMap<>();
        entries.forEach(entry -> map.put(entry.getKey(), entry.getValue()));
        return map;
    }

    private static void permute(List<Map.Entry<String, String>> entries, int from, List<Map<String, String>> orders) {
        if (from == entries.size()) {
            orders.add(asMap(entries));
            return;
        }
        for (int i = from; i < entries.size(); i++) {
            Collections.swap(entries, from, i);
            permute(entries, from + 1, orders);
            Collections.swap(entries, from, i);
        }
    }
}
