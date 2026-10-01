package org.open4goods.datareference.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.grouping.GroupId;
import org.open4goods.datareference.model.grouping.ModelPatternRuleRegistry;
import org.open4goods.datareference.model.projection.GroupAssignment;
import org.open4goods.datareference.model.resolution.ResolvedValue;
import org.open4goods.datareference.model.value.CanonicalValue;
import org.open4goods.datareference.model.value.CodeValue;
import org.open4goods.datareference.model.value.LocalizedTextValue;
import org.open4goods.datareference.port.ModelGroupingPort;

/**
 * Adds the reviewed-pattern-rule {@code FAMILY} group on top of a delegate's
 * model grouping (AC3/AC4 of GOU-49).
 *
 * <p>Decorates rather than replaces its delegate -- typically
 * {@link CompositeModelGroupingService}, itself composing
 * {@link ExactTupleModelGroupingService} and
 * {@link ExplicitRelationModelGroupingService}: this service only ever
 * assembles what the delegate already confirmed, plus at most one additional
 * reviewed family group. A {@link org.open4goods.datareference.model.grouping.ModelPatternRule}
 * match confirms a family group outright because every rule this engine can
 * even load is, by construction, the reviewed branch of ADR-0010. This
 * service deliberately implements no unreviewed common-prefix similarity:
 * that candidate-only signal is a separate mechanism and must never be
 * expressed as a confirmed membership returned here.
 */
public final class PatternRuleFamilyGroupingService implements ModelGroupingPort {

    private final ModelGroupingPort delegate;
    private final ModelPatternRuleRegistry familyRules;
    private final CanonicalAttributeId brandAttribute;
    private final CanonicalAttributeId modelAttribute;

    /**
     * Creates the decorator.
     *
     * @param delegate grouping port supplying the confirmed model group and search tokens
     * @param familyRules compiled, reviewed pattern rules
     * @param brandAttribute canonical attribute that carries the resolved brand
     * @param modelAttribute canonical attribute that carries the resolved full model
     */
    public PatternRuleFamilyGroupingService(ModelGroupingPort delegate, ModelPatternRuleRegistry familyRules,
            CanonicalAttributeId brandAttribute, CanonicalAttributeId modelAttribute) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.familyRules = Objects.requireNonNull(familyRules, "familyRules must not be null");
        this.brandAttribute = Objects.requireNonNull(brandAttribute, "brandAttribute must not be null");
        this.modelAttribute = Objects.requireNonNull(modelAttribute, "modelAttribute must not be null");
    }

    @Override
    public GroupAssignment assignGroups(Gtin gtin, ProjectionSurface surface, List<SourceRecordHead> sourceHeads,
            List<ResolvedValue> resolvedValues, Optional<CanonicalClassId> resolvedClass) {
        GroupAssignment base = delegate.assignGroups(gtin, surface, sourceHeads, resolvedValues, resolvedClass);
        if (resolvedClass.isEmpty()) {
            return base;
        }
        Optional<String> brandText = findText(resolvedValues, brandAttribute);
        Optional<String> modelText = findText(resolvedValues, modelAttribute);
        if (brandText.isEmpty() || modelText.isEmpty()) {
            return base;
        }
        Optional<GroupId> family = familyRules.matchFamily(brandText.get(), resolvedClass.get(), modelText.get());
        if (family.isEmpty()) {
            return base;
        }
        List<GroupId> families = new ArrayList<>(base.familyGroupIds());
        families.add(family.get());
        return new GroupAssignment(base.modelGroupId(), families, base.searchTokens());
    }

    private Optional<String> findText(List<ResolvedValue> resolvedValues, CanonicalAttributeId attribute) {
        return resolvedValues.stream()
                .filter(resolved -> resolved.attribute().equals(attribute))
                .findFirst()
                .map(ResolvedValue::value)
                .flatMap(PatternRuleFamilyGroupingService::asText);
    }

    private static Optional<String> asText(CanonicalValue value) {
        if (value instanceof LocalizedTextValue text) {
            return Optional.of(text.value());
        }
        if (value instanceof CodeValue code) {
            return Optional.of(code.value());
        }
        return Optional.empty();
    }
}
