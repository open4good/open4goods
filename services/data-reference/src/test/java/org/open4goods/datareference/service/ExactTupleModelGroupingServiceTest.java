package org.open4goods.datareference.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.AssertionId;
import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.grouping.GroupId;
import org.open4goods.datareference.model.grouping.GroupType;
import org.open4goods.datareference.model.projection.GroupAssignment;
import org.open4goods.datareference.model.resolution.ResolutionReason;
import org.open4goods.datareference.model.resolution.ResolvedValue;
import org.open4goods.datareference.model.value.CodeValue;
import org.open4goods.datareference.model.value.LocalizedTextValue;
import org.open4goods.datareference.model.LanguageTag;

/**
 * Tests AC2 of GOU-49: automatic model grouping is limited to the exact
 * normalized tuple of canonical brand, O4G class and full model, and a
 * missing component always suppresses it (fixtures cover AC8 punctuation,
 * case, identical prefixes across classes and arrival-order independence).
 */
class ExactTupleModelGroupingServiceTest {

    private static final Gtin GTIN = new Gtin("4006381333931");
    private static final CanonicalAttributeId BRAND = new CanonicalAttributeId("brand");
    private static final CanonicalAttributeId MODEL = new CanonicalAttributeId("model");
    private static final CanonicalClassId TV_CLASS = new CanonicalClassId("television");
    private static final CanonicalClassId FRIDGE_CLASS = new CanonicalClassId("refrigerator");

    private final ExactTupleModelGroupingService service = new ExactTupleModelGroupingService(BRAND, MODEL);

    @Test
    void assignsTheSameModelGroupRegardlessOfPunctuationCaseOrArrivalOrder() {
        GroupAssignment first = service.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB,
                List.of(brand("Acme"), model("XR-500")), Optional.of(TV_CLASS));
        GroupAssignment second = service.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB,
                List.of(model("xr 500"), brand("ACME")), Optional.of(TV_CLASS));

        assertThat(first.modelGroup()).contains(new GroupId(GroupType.MODEL, "television-acme-xr-500"));
        assertThat(second.modelGroup()).isEqualTo(first.modelGroup());
    }

    @Test
    void identicalBrandAndModelInADifferentClassProduceADifferentGroup() {
        GroupAssignment tv = service.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB,
                List.of(brand("Acme"), model("X1")), Optional.of(TV_CLASS));
        GroupAssignment fridge = service.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB,
                List.of(brand("Acme"), model("X1")), Optional.of(FRIDGE_CLASS));

        assertThat(tv.modelGroup()).isNotEqualTo(fridge.modelGroup());
    }

    @Test
    void missingBrandSuppressesAutomaticGrouping() {
        GroupAssignment assignment = service.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB,
                List.of(model("XR-500")), Optional.of(TV_CLASS));

        assertThat(assignment.modelGroup()).isEmpty();
        assertThat(assignment.searchTokens()).containsExactly("xr", "500");
    }

    @Test
    void missingClassSuppressesAutomaticGrouping() {
        GroupAssignment assignment = service.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB,
                List.of(brand("Acme"), model("XR-500")), Optional.empty());

        assertThat(assignment.modelGroup()).isEmpty();
    }

    @Test
    void missingModelSuppressesAutomaticGroupingAndProducesNoSearchTokens() {
        GroupAssignment assignment = service.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB,
                List.of(brand("Acme")), Optional.of(TV_CLASS));

        assertThat(assignment.modelGroup()).isEmpty();
        assertThat(assignment.searchTokens()).isEmpty();
    }

    @Test
    void aSizeEmbeddedInTheModelCodeIsPartOfTheExactTupleNotStripped() {
        GroupAssignment with55 = service.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB,
                List.of(brand("Acme"), model("TV55")), Optional.of(TV_CLASS));
        GroupAssignment with65 = service.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB,
                List.of(brand("Acme"), model("TV65")), Optional.of(TV_CLASS));

        assertThat(with55.modelGroup()).isNotEqualTo(with65.modelGroup());
    }

    @Test
    void anUnsupportedResolvedValueTypeIsTreatedAsAbsent() {
        ResolvedValue nonTextBrand = resolved(BRAND, new org.open4goods.datareference.model.value.IntegerValue(
                java.math.BigInteger.valueOf(7)));
        GroupAssignment assignment = service.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB,
                List.of(nonTextBrand, model("XR-500")), Optional.of(TV_CLASS));

        assertThat(assignment.modelGroup()).isEmpty();
    }

    @Test
    void aCodeValueIsUsableAsExactTupleText() {
        ResolvedValue codedBrand = resolved(BRAND, new CodeValue("brand-registry", "Acme"));
        GroupAssignment assignment = service.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB,
                List.of(codedBrand, model("XR-500")), Optional.of(TV_CLASS));

        assertThat(assignment.modelGroup()).contains(new GroupId(GroupType.MODEL, "television-acme-xr-500"));
    }

    private static ResolvedValue brand(String value) {
        return resolved(BRAND, new LocalizedTextValue(value, LanguageTag.UND));
    }

    private static ResolvedValue model(String value) {
        return resolved(MODEL, new LocalizedTextValue(value, LanguageTag.UND));
    }

    private static ResolvedValue resolved(CanonicalAttributeId attribute,
            org.open4goods.datareference.model.value.CanonicalValue value) {
        AssertionId assertionId = new AssertionId("urn:o4g:assertion:" + attribute.slug());
        return new ResolvedValue(attribute, value, assertionId, List.of(assertionId),
                new RuleVersion("resolution", 1), ResolutionReason.CONFIGURED_SOURCE_RANK, false);
    }
}
