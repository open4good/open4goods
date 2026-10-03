package org.open4goods.datareference.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.grouping.GroupId;
import org.open4goods.datareference.model.grouping.GroupType;
import org.open4goods.datareference.model.grouping.ModelTextNormalizer;
import org.open4goods.datareference.model.projection.GroupAssignment;
import org.open4goods.datareference.model.resolution.ResolvedValue;
import org.open4goods.datareference.model.value.CanonicalValue;
import org.open4goods.datareference.model.value.CodeValue;
import org.open4goods.datareference.model.value.LocalizedTextValue;
import org.open4goods.datareference.port.ModelGroupingPort;

/**
 * Assigns a confirmed model group only from the exact normalized tuple of
 * canonical brand, O4G class and full model (ADR-0010, AC2 of GOU-49).
 *
 * <p>A missing brand, class or model deterministically suppresses grouping
 * rather than falling back to a weaker match. Explicit resolved source
 * relations (the other AC2 path) are a separate port implementation,
 * {@link ExplicitRelationModelGroupingService}, composed alongside this one
 * by {@link CompositeModelGroupingService}; this service ignores
 * {@code sourceHeads} entirely. Reviewed pattern rules and candidate-only
 * prefix similarity are also out of scope for this port and it always returns
 * no family memberships.
 */
public final class ExactTupleModelGroupingService implements ModelGroupingPort {

    private final CanonicalAttributeId brandAttribute;
    private final CanonicalAttributeId modelAttribute;

    /**
     * Creates the exact-tuple grouping service.
     *
     * @param brandAttribute canonical attribute that carries the resolved brand
     * @param modelAttribute canonical attribute that carries the resolved full model
     */
    public ExactTupleModelGroupingService(CanonicalAttributeId brandAttribute, CanonicalAttributeId modelAttribute) {
        this.brandAttribute = Objects.requireNonNull(brandAttribute, "brandAttribute must not be null");
        this.modelAttribute = Objects.requireNonNull(modelAttribute, "modelAttribute must not be null");
    }

    @Override
    public GroupAssignment assignGroups(Gtin gtin, ProjectionSurface surface, List<SourceRecordHead> sourceHeads,
            List<ResolvedValue> resolvedValues, Optional<CanonicalClassId> resolvedClass) {
        Objects.requireNonNull(gtin, "gtin must not be null");
        Objects.requireNonNull(surface, "surface must not be null");
        Objects.requireNonNull(sourceHeads, "sourceHeads must not be null");
        Objects.requireNonNull(resolvedValues, "resolvedValues must not be null");
        Objects.requireNonNull(resolvedClass, "resolvedClass must not be null");

        Optional<String> brandText = findText(resolvedValues, brandAttribute);
        Optional<String> modelText = findText(resolvedValues, modelAttribute);

        List<String> searchTokens = modelText.map(ModelTextNormalizer::tokenize).orElse(List.of());

        String normalizedBrand = brandText.map(ModelTextNormalizer::normalize).orElse("");
        String normalizedModel = modelText.map(ModelTextNormalizer::normalize).orElse("");

        if (normalizedBrand.isEmpty() || normalizedModel.isEmpty() || resolvedClass.isEmpty()) {
            return new GroupAssignment(null, List.of(), searchTokens);
        }

        String slug = ModelTextNormalizer.joinSlug(resolvedClass.orElseThrow().slug(), normalizedBrand, normalizedModel);
        GroupId modelGroupId = new GroupId(GroupType.MODEL, slug);
        return new GroupAssignment(modelGroupId, List.of(), searchTokens);
    }

    private Optional<String> findText(List<ResolvedValue> resolvedValues, CanonicalAttributeId attribute) {
        return resolvedValues.stream()
                .filter(resolved -> resolved.attribute().equals(attribute))
                .findFirst()
                .map(ResolvedValue::value)
                .flatMap(ExactTupleModelGroupingService::asText);
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
