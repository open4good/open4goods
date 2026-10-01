package org.open4goods.datareference.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.GtinLink;
import org.open4goods.datareference.model.GtinMatchConfidence;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.SourceAssertion;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.evidence.RelationEvidence;
import org.open4goods.datareference.model.evidence.SourceEvidence;
import org.open4goods.datareference.model.evidence.SourceEvidenceKind;
import org.open4goods.datareference.model.grouping.GroupId;
import org.open4goods.datareference.model.grouping.GroupType;
import org.open4goods.datareference.model.projection.GroupAssignment;
import org.open4goods.datareference.model.resolution.ResolvedValue;
import org.open4goods.datareference.port.ModelGroupingPort;
import org.open4goods.datareference.port.SourceRecordHeadStore;

/**
 * Confirms model/family membership from an explicit provider relation -- as
 * strong evidence as the exact normalized tuple (ADR-0010; remaining AC2 and
 * the explicit-relation part of AC3 of GOU-49).
 *
 * <p>A relation is only followed when its target can be resolved without
 * guessing: either {@code targetScheme} is {@code "gtin"} (the identifier is
 * itself a GTIN), or it is {@code "<sourceId>-record"} for the very source
 * that asserted the relation, a same-catalogue cross-reference resolved
 * through {@link SourceRecordHeadStore#find}. Any other scheme is left
 * unresolved rather than mapped through an assumed registry. Among the
 * relation types this service recognizes, {@code variant-of} and
 * {@code same-model-as} confirm MODEL membership; {@code family-of},
 * {@code series-of}, {@code same-family-as} and {@code same-series-as}
 * confirm FAMILY membership. Any other relation type (an accessory link, for
 * instance) carries no grouping weight.
 *
 * <p>The confirmed group id is built from the two linked GTINs, sorted and
 * joined by a hyphen: deterministic regardless of which GTIN is being
 * rebuilt and independent of arrival order against the exact-tuple path
 * (GOU-49 AC8).
 */
public final class ExplicitRelationModelGroupingService implements ModelGroupingPort {

    private static final Set<String> MODEL_RELATION_TYPES = Set.of("variant-of", "same-model-as");
    private static final Set<String> FAMILY_RELATION_TYPES =
            Set.of("family-of", "series-of", "same-family-as", "same-series-as");
    private static final String GTIN_SCHEME = "gtin";
    private static final String RECORD_SCHEME_SUFFIX = "-record";

    private final SourceRecordHeadStore sourceHeadStore;

    /**
     * Creates the explicit-relation grouping service.
     *
     * @param sourceHeadStore store used to resolve a same-catalogue relation target
     */
    public ExplicitRelationModelGroupingService(SourceRecordHeadStore sourceHeadStore) {
        this.sourceHeadStore = Objects.requireNonNull(sourceHeadStore, "sourceHeadStore must not be null");
    }

    @Override
    public GroupAssignment assignGroups(Gtin gtin, ProjectionSurface surface, List<SourceRecordHead> sourceHeads,
            List<ResolvedValue> resolvedValues, Optional<CanonicalClassId> resolvedClass) {
        Objects.requireNonNull(gtin, "gtin must not be null");
        Objects.requireNonNull(surface, "surface must not be null");
        Objects.requireNonNull(sourceHeads, "sourceHeads must not be null");
        Objects.requireNonNull(resolvedValues, "resolvedValues must not be null");
        Objects.requireNonNull(resolvedClass, "resolvedClass must not be null");

        GroupId modelGroupId = null;
        List<GroupId> familyGroupIds = new ArrayList<>();

        for (SourceRecordHead head : sourceHeads) {
            for (SourceAssertion assertion : head.assertions()) {
                SourceEvidence evidence = assertion.evidence();
                if (evidence.kind() != SourceEvidenceKind.RELATION) {
                    continue;
                }
                RelationEvidence relation = (RelationEvidence) evidence;
                Optional<Gtin> target = resolveTarget(head.key(), relation);
                if (target.isEmpty() || target.get().equals(gtin)) {
                    continue;
                }
                String pairSlug = pairSlug(gtin, target.get());
                if (modelGroupId == null && MODEL_RELATION_TYPES.contains(relation.relationType())) {
                    modelGroupId = new GroupId(GroupType.MODEL, pairSlug);
                } else if (FAMILY_RELATION_TYPES.contains(relation.relationType())) {
                    familyGroupIds.add(new GroupId(GroupType.FAMILY, pairSlug));
                }
            }
        }

        return new GroupAssignment(modelGroupId, familyGroupIds, List.of());
    }

    private Optional<Gtin> resolveTarget(SourceRecordKey ownKey, RelationEvidence relation) {
        if (GTIN_SCHEME.equals(relation.targetScheme())) {
            return parseGtin(relation.targetIdentifier());
        }
        String ownRecordScheme = ownKey.sourceId().value() + RECORD_SCHEME_SUFFIX;
        if (!ownRecordScheme.equals(relation.targetScheme())) {
            return Optional.empty();
        }
        SourceRecordKey targetKey = SourceRecordKey.of(ownKey.sourceId().value(), relation.targetIdentifier());
        return sourceHeadStore.find(targetKey).flatMap(this::strongestGtin);
    }

    private Optional<Gtin> parseGtin(String value) {
        try {
            return Optional.of(new Gtin(value));
        } catch (IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }

    private Optional<Gtin> strongestGtin(SourceRecordHead head) {
        return head.gtinLinks().stream()
                .min((first, second) -> confidenceRank(first.confidence()) - confidenceRank(second.confidence()))
                .map(GtinLink::gtin);
    }

    private int confidenceRank(GtinMatchConfidence confidence) {
        return switch (confidence) {
            case EXACT -> 0;
            case STRONG -> 1;
            case WEAK -> 2;
            case UNVERIFIED -> 3;
        };
    }

    private String pairSlug(Gtin first, Gtin second) {
        String firstValue = first.value();
        String secondValue = second.value();
        return firstValue.compareTo(secondValue) <= 0 ? firstValue + "-" + secondValue : secondValue + "-" + firstValue;
    }
}
