package org.open4goods.datareference.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.AssertionId;
import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.LanguageTag;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.grouping.GroupId;
import org.open4goods.datareference.model.grouping.GroupType;
import org.open4goods.datareference.model.grouping.ModelPatternRule;
import org.open4goods.datareference.model.grouping.ModelPatternRuleRegistry;
import org.open4goods.datareference.model.projection.GroupAssignment;
import org.open4goods.datareference.model.resolution.ResolutionReason;
import org.open4goods.datareference.model.resolution.ResolvedValue;
import org.open4goods.datareference.model.value.LocalizedTextValue;

/**
 * Tests that a reviewed pattern rule confirms a FAMILY group on top of the
 * exact-tuple model group, and that it never does so without a confirmed
 * class, brand or model (GOU-198, AC3/AC4 of GOU-49).
 */
class PatternRuleFamilyGroupingServiceTest {

    private static final Gtin GTIN = new Gtin("4006381333931");
    private static final CanonicalAttributeId BRAND = new CanonicalAttributeId("brand");
    private static final CanonicalAttributeId MODEL = new CanonicalAttributeId("model");
    private static final CanonicalClassId TV = new CanonicalClassId("television");

    private final ExactTupleModelGroupingService exactTuple = new ExactTupleModelGroupingService(BRAND, MODEL);
    private final ModelPatternRuleRegistry familyRules = new ModelPatternRuleRegistry(List.of(
            new ModelPatternRule(new RuleVersion("family-acme-xr-television", 1), "acme", TV,
                    "(?i)^(?<family>xr)[- ]?(?<model>\\d{3,4})(?:[- ]?(?:eu|fr|us|uk))?$",
                    List.of("XR-500-EU", "XR-650-EU"), List.of("QX-500-EU"), "catalog-review@open4goods.org")));
    private final PatternRuleFamilyGroupingService service = new PatternRuleFamilyGroupingService(exactTuple,
            familyRules, BRAND, MODEL);

    @Test
    void confirmsTheReviewedFamilyGroupAlongsideTheExactTupleModelGroup() {
        GroupAssignment assignment = service.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB, List.of(),
                List.of(brand("Acme"), model("XR-500-EU")), Optional.of(TV));

        assertThat(assignment.modelGroup()).contains(new GroupId(GroupType.MODEL, "10-television-4-acme-9-xr-500-eu"));
        assertThat(assignment.familyGroupIds()).containsExactly(new GroupId(GroupType.FAMILY, "10-television-4-acme-2-xr"));
    }

    @Test
    void differentSizesInTheModelCodeShareTheSameConfirmedFamily() {
        GroupAssignment size500 = service.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB, List.of(),
                List.of(brand("Acme"), model("XR-500-EU")), Optional.of(TV));
        GroupAssignment size650 = service.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB, List.of(),
                List.of(brand("Acme"), model("XR-650-EU")), Optional.of(TV));

        assertThat(size500.familyGroupIds()).isEqualTo(size650.familyGroupIds());
        assertThat(size500.modelGroup()).isNotEqualTo(size650.modelGroup());
    }

    @Test
    void noMatchingRuleLeavesTheFamilyGroupsEmpty() {
        GroupAssignment assignment = service.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB, List.of(),
                List.of(brand("Acme"), model("QX-500-EU")), Optional.of(TV));

        assertThat(assignment.familyGroupIds()).isEmpty();
    }

    @Test
    void aMissingClassSuppressesTheFamilyGroupEvenIfTextWouldOtherwiseMatch() {
        GroupAssignment assignment = service.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB, List.of(),
                List.of(brand("Acme"), model("XR-500-EU")), Optional.empty());

        assertThat(assignment.familyGroupIds()).isEmpty();
        assertThat(assignment.modelGroup()).isEmpty();
    }

    @Test
    void aMissingBrandSuppressesTheFamilyGroup() {
        GroupAssignment assignment = service.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB, List.of(),
                List.of(model("XR-500-EU")), Optional.of(TV));

        assertThat(assignment.familyGroupIds()).isEmpty();
    }

    private static ResolvedValue brand(String value) {
        return resolved(BRAND, value);
    }

    private static ResolvedValue model(String value) {
        return resolved(MODEL, value);
    }

    private static ResolvedValue resolved(CanonicalAttributeId attribute, String value) {
        AssertionId assertionId = new AssertionId("urn:o4g:assertion:" + attribute.slug());
        return new ResolvedValue(attribute, new LocalizedTextValue(value, LanguageTag.UND), assertionId,
                List.of(assertionId), new RuleVersion("resolution", 1), ResolutionReason.CONFIGURED_SOURCE_RANK, false);
    }
}
