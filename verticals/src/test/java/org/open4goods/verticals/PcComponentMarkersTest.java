package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.model.product.ProductNature;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * The PC component markers of the vote (GOU-2317) leave the nature of 4e (GOU-2233) and the historical rule unchanged.
 *
 * <p>They are in {@code sparePartCategoryMarkers}, so a PC component row votes against a vertical, and in
 * {@code natureNeutralCategoryFragments}, so the nature removes them before it searches the markers. The nature
 * recovery (GOU-2283, GOU-2284) runs with the lists of {@code main} before this change: the nature must stay
 * strictly the same. Two service instances: {@code shipped} reads {@code _default.yml} as shipped, {@code before} is
 * the same configuration without the PC component markers, in the three lists.
 */
class PcComponentMarkersTest {

    /** The markers added by GOU-2317, for the vote only. */
    static final Set<String> PC_COMPONENT_MARKERS = Set.of("BOITIER PC", "COMPOSANT PC", "COMPOSANTS PC",
            "INFORMATIQUE | COMPOSANTS |");

    private static final List<Double> THRESHOLDS = List.of(0d, 0.5d, 1d);

    /** Separators met in merchant categories, and none (two tokens glued). */
    private static final List<String> SEPARATORS = List.of(" > ", " | ", " / ", " - ", ">", " ", "");

    private static VerticalsConfigService shipped;
    private static VerticalsConfigService before;
    private static Map<String, CategoryVoteReplay.ExportedProduct> realCategories;

    @BeforeAll
    static void setUp() throws Exception {
        shipped = service();
        before = service();
        // The comparisons below read the historical rule; the vote is on since GOU-2317
        shipped.getDefaultConfig().setCategoryVoteEnabled(false);
        before.getDefaultConfig().setCategoryVoteEnabled(false);
        VerticalConfig defaults = before.getDefaultConfig();
        defaults.getSparePartCategoryMarkers().removeAll(PC_COMPONENT_MARKERS);
        defaults.getAccessoryCategoryMarkers().removeAll(PC_COMPONENT_MARKERS);
        defaults.getNatureNeutralCategoryFragments().removeAll(PC_COMPONENT_MARKERS);
        realCategories = CategoryVoteTest.readRealCategories();
    }

    private static VerticalsConfigService service() {
        return new VerticalsConfigService(new SerialisationService(), mock(GoogleTaxonomyService.class),
                new PathMatchingResourcePatternResolver());
    }

    @Test
    void everyPcComponentMarkerIsAVoteMarkerAndANeutralFragment() {
        VerticalConfig defaults = shipped.getDefaultConfig();
        assertThat(defaults.getSparePartCategoryMarkers()).containsAll(PC_COMPONENT_MARKERS);
        assertThat(defaults.getNatureNeutralCategoryFragments()).containsAll(PC_COMPONENT_MARKERS);
        assertThat(defaults.getAccessoryCategoryMarkers()).doesNotContainAnyElementsOf(PC_COMPONENT_MARKERS);
        assertThat(service().isCategoryVoteEnabled()).as("switched on by GOU-2317").isTrue();

        for (String marker : PC_COMPONENT_MARKERS) {
            assertThat(shipped.carriesNatureMarker("INFORMATIQUE > " + marker + " > LEAF")).as(marker).isTrue();
            assertThat(before.carriesNatureMarker("INFORMATIQUE > " + marker + " > LEAF")).as(marker).isFalse();
        }
    }

    /**
     * The nature removes the neutral fragments longest first. A PC component marker could only change the nature of a
     * category if its removal cut an occurrence of another marker or fragment, or joined one: it neither contains
     * nor is contained in any of them. The only overlaps are one shared letter between the {@code C} ending
     * {@code ... PC} and a token starting with {@code C} ({@code COMPOSANT ...}, {@code CHAUSSURES ...}), which would
     * need two words glued without separator ({@code ...PCOMPOSANT...}), not a merchant category.
     */
    @Test
    void pcComponentMarkersDoNotOverlapOtherMarkersOrFragments() {
        Set<String> others = new LinkedHashSet<>();
        VerticalConfig defaults = shipped.getDefaultConfig();
        others.addAll(defaults.getSparePartCategoryMarkers());
        others.addAll(defaults.getAccessoryCategoryMarkers());
        others.addAll(defaults.getNatureNeutralCategoryFragments());

        for (String marker : PC_COMPONENT_MARKERS) {
            for (String other : others) {
                if (other.equals(marker)) {
                    continue;
                }
                assertThat(marker).as("%s / %s", marker, other).doesNotContain(other);
                assertThat(other).as("%s / %s", other, marker).doesNotContain(marker);
                for (String[] pair : List.of(new String[] { marker, other }, new String[] { other, marker })) {
                    for (int k = 1; k < Math.min(pair[0].length(), pair[1].length()); k++) {
                        if (pair[0].endsWith(pair[1].substring(0, k))) {
                            String glued = pair[0] + pair[1].substring(k);
                            assertThat(k).as("overlap of %s and %s in %s", pair[0], pair[1], glued).isEqualTo(1);
                            int at = pair[0].length() - 1;
                            assertThat(Character.isLetter(glued.charAt(at - 1)) && Character.isLetter(glued.charAt(at + 1)))
                                    .as("%s glues two words", glued).isTrue();
                        }
                    }
                }
            }
        }
    }

    /** Every real category of the card, alone and as the bag of its product, at every threshold. */
    @Test
    void natureUnchangedOnTheRealCategories() {
        for (Map.Entry<String, CategoryVoteReplay.ExportedProduct> product : realCategories.entrySet()) {
            Collection<String> bag = product.getValue().categoriesByDatasources().values();
            assertSameNature(product.getKey(), bag);
            bag.forEach(category -> assertSameNature(product.getKey(), List.of(category)));
        }
    }

    /** Every {@code matchingCategories} entry of every vertical. */
    @Test
    void natureUnchangedOnTheMatchingCategories() {
        for (VerticalConfig vertical : shipped.getConfigsWithoutDefault()) {
            vertical.getMatchingCategories().values().stream().filter(categories -> categories != null)
                    .flatMap(Collection::stream)
                    .forEach(category -> assertSameNature(vertical.getId(), List.of(category)));
        }
    }

    /**
     * Each PC component marker next to each other marker, each neutral fragment and plain words, on both sides,
     * with every separator, glued included.
     */
    @Test
    void natureUnchangedOnCombinations() {
        Set<String> neighbours = new LinkedHashSet<>();
        VerticalConfig defaults = shipped.getDefaultConfig();
        neighbours.addAll(defaults.getSparePartCategoryMarkers());
        neighbours.addAll(defaults.getAccessoryCategoryMarkers());
        neighbours.addAll(defaults.getNatureNeutralCategoryFragments());
        neighbours.addAll(List.of("CARTE GRAPHIQUE", "INFORMATIQUE ET BUREAU", "ACCESSOIRES PC", "PIECES DETACHEES PC",
                "HOUSSE", "65 | INPUT DEVICES | INFORMATIQUE"));

        List<String> combinations = new ArrayList<>();
        for (String marker : PC_COMPONENT_MARKERS) {
            for (String neighbour : neighbours) {
                for (String separator : SEPARATORS) {
                    combinations.add(marker + separator + neighbour);
                    combinations.add(neighbour + separator + marker);
                    combinations.add("INFORMATIQUE > " + marker + separator + neighbour + " > LEAF");
                    combinations.add(neighbour + separator + marker + separator + neighbour);
                }
            }
        }
        assertThat(combinations).hasSizeGreaterThan(1000);
        combinations.forEach(category -> assertSameNature("combination", List.of(category)));
        // Lower-case forms, upper-cased by the normalisation of the nature
        combinations.forEach(category -> assertSameNature("lower case", List.of(category.toLowerCase())));
    }

    /** A category carrying a PC component marker and a real marker keeps the nature of the real marker. */
    @Test
    void realMarkerNextToAPcComponentMarkerKeepsItsNature() {
        Map<String, ProductNature> expected = new LinkedHashMap<>();
        expected.put("BOITIER PC > ACCESSOIRE BOITIER PC", ProductNature.ACCESSORY);
        expected.put("INFORMATIQUE ET BUREAU > COMPOSANT PC > HOUSSE", ProductNature.ACCESSORY);
        expected.put("INFORMATIQUE | COMPOSANTS | PIECES DETACHEES", ProductNature.SPARE_PART);
        expected.put("INFORMATIQUE / COMPOSANTS PC / PIECE DETACHEE VENTILATEUR", ProductNature.SPARE_PART);
        expected.put("INFORMATIQUE ET BUREAU > COMPOSANT PC > BOITIER PC", null);
        expected.put("INFORMATIQUE | COMPOSANTS | BOITIER", null);
        expected.put("INFORMATIQUE > COMPOSANT - PIECE DETACHEE > BUREAU BOITIER PC", null);
        expected.forEach((category, nature) -> {
            assertThat(shipped.getNatureForCategories(List.of(category))).as(category).isEqualTo(nature);
            assertSameNature(category, List.of(category));
        });
    }

    /**
     * The historical rule reads only the {@code matchingCategories} and the {@code excludingTokensFromCategoriesMatching}
     * of the verticals: with the flag off, the PC component markers change nothing.
     */
    @Test
    void historicalRuleUnchanged() {
        for (Map.Entry<String, CategoryVoteReplay.ExportedProduct> product : realCategories.entrySet()) {
            Map<String, String> categories = product.getValue().categoriesByDatasources();
            for (String eprelGroup : new String[] { null, product.getValue().eprelGroup() }) {
                assertThat(id(shipped.getVerticalForCategories(categories, eprelGroup))).as(product.getKey())
                        .isEqualTo(id(before.getVerticalForCategories(categories, eprelGroup)));
            }
            categories.forEach((datasource, category) -> assertThat(
                    id(shipped.getVerticalForCategories(Map.of(datasource, category))))
                    .as("%s %s", product.getKey(), datasource)
                    .isEqualTo(id(before.getVerticalForCategories(Map.of(datasource, category)))));
        }
    }

    private static void assertSameNature(String what, Collection<String> categories) {
        for (double threshold : THRESHOLDS) {
            assertThat(shipped.getNatureForCategories(categories, threshold))
                    .as("%s, threshold %s: %s", what, threshold, categories)
                    .isEqualTo(before.getNatureForCategories(categories, threshold));
        }
    }

    private static String id(VerticalConfig vertical) {
        return vertical == null ? null : vertical.getId();
    }
}
