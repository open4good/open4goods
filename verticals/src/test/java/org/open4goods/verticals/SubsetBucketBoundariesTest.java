package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.SubsetCriteria;
import org.open4goods.model.vertical.SubsetCriteriaOperator;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.VerticalSubCategory;
import org.open4goods.model.vertical.VerticalSubset;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * {@code LOWER_THAN} and {@code GREATER_THAN} are strict bounds (GOU-2586): a bucket ending with
 * {@code LOWER_THAN b} next to a bucket starting with {@code GREATER_THAN b} leaves the products at exactly
 * {@code b} out of both. Adjacent buckets must be half-open, the next one starting with
 * {@code GREATER_THAN_OR_EQUAL b}.
 */
class SubsetBucketBoundariesTest {

    private static VerticalsConfigService service;

    @BeforeAll
    static void setUp() throws Exception {
        service = new VerticalsConfigService(new SerialisationService(), mock(GoogleTaxonomyService.class),
                new PathMatchingResourcePatternResolver());
    }

    @Test
    void adjacentBucketsLeaveNoValueOut() {
        List<String> gaps = new ArrayList<>();
        for (VerticalConfig config : service.getConfigs().values()) {
            collectGaps(config.getId() + " subsets", subsetCriteria(config.getSubsets()), gaps);
            if (config.getNudgeToolConfig() != null) {
                collectGaps(config.getId() + " nudgeToolConfig.subsets",
                        subsetCriteria(config.getNudgeToolConfig().getSubsets()), gaps);
            }
            if (config.getSubCategories() != null) {
                Map<String, List<SubsetCriteria>> byKey = new LinkedHashMap<>();
                for (VerticalSubCategory subCategory : config.getSubCategories()) {
                    if (subCategory.getActivatedFilters() != null) {
                        subCategory.getActivatedFilters()
                                .forEach(c -> byKey.computeIfAbsent(c.getField(), k -> new ArrayList<>()).add(c));
                    }
                }
                collectGaps(config.getId() + " subCategories", byKey, gaps);
            }
        }
        assertThat(gaps).isEmpty();
    }

    @Test
    void washingMachineSubCategoriesSplitAtEightAndTenKilos() {
        VerticalConfig config = service.getConfigById("washing-machine");
        Map<String, List<String>> bounds = new LinkedHashMap<>();
        for (VerticalSubCategory subCategory : config.getSubCategories()) {
            bounds.put(subCategory.getId(), subCategory.getActivatedFilters().stream()
                    .map(c -> c.getOperator() + " " + c.getValue()).toList());
        }
        assertThat(bounds).containsEntry("washing_machine_under_8_kg", List.of("LOWER_THAN 8"))
                .containsEntry("washing_machine_8_9_kg", List.of("GREATER_THAN_OR_EQUAL 8", "LOWER_THAN 10"))
                .containsEntry("washing_machine_10_kg_and_more", List.of("GREATER_THAN_OR_EQUAL 10"));
    }

    /**
     * Groups the criteria of the subsets by bucket group and field.
     */
    private static Map<String, List<SubsetCriteria>> subsetCriteria(List<VerticalSubset> subsets) {
        Map<String, List<SubsetCriteria>> byKey = new LinkedHashMap<>();
        if (subsets == null) {
            return byKey;
        }
        for (VerticalSubset subset : subsets) {
            if (subset.getCriterias() != null) {
                subset.getCriterias().forEach(c -> byKey
                        .computeIfAbsent(subset.getGroup() + " / " + c.getField(), k -> new ArrayList<>()).add(c));
            }
        }
        return byKey;
    }

    /**
     * Records every value used both as a {@code LOWER_THAN} and as a {@code GREATER_THAN} bound of the same key.
     */
    private static void collectGaps(String scope, Map<String, List<SubsetCriteria>> byKey, List<String> gaps) {
        byKey.forEach((key, criterias) -> {
            Set<String> upper = new LinkedHashSet<>();
            Set<String> lower = new LinkedHashSet<>();
            for (SubsetCriteria criteria : criterias) {
                if (criteria.getOperator() == SubsetCriteriaOperator.LOWER_THAN) {
                    upper.add(criteria.getValue());
                } else if (criteria.getOperator() == SubsetCriteriaOperator.GREATER_THAN) {
                    lower.add(criteria.getValue());
                }
            }
            upper.retainAll(lower);
            upper.forEach(value -> gaps.add(scope + " / " + key + " leaves out " + value));
        });
    }
}
