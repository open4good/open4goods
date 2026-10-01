package org.open4goods.datareference.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.grouping.GroupId;
import org.open4goods.datareference.model.projection.GroupAssignment;
import org.open4goods.datareference.model.resolution.ResolvedValue;
import org.open4goods.datareference.port.ModelGroupingPort;

/**
 * Composes the exact-tuple and explicit-relation model grouping paths
 * (GOU-49 AC2): either path can confirm a MODEL group, and composing them
 * never silently drops what the other confirmed.
 *
 * <p>When both paths confirm a MODEL group for the same GTIN, the exact
 * tuple's id is kept: reconciling two different ids for the same leaf is a
 * group-merge decision that belongs to the group index (GOU-199), not to
 * per-rebuild assignment. This choice is fixed regardless of which path is
 * invoked first, so the composed result is arrival-order independent
 * (GOU-49 AC8). Family memberships and search tokens are the union of both
 * paths, since neither of those is single-valued.
 */
public final class CompositeModelGroupingService implements ModelGroupingPort {

    private final ModelGroupingPort exactTuple;
    private final ModelGroupingPort explicitRelation;

    /**
     * Creates the composite grouping service.
     *
     * @param exactTuple the exact normalized tuple path
     * @param explicitRelation the explicit provider relation path
     */
    public CompositeModelGroupingService(ModelGroupingPort exactTuple, ModelGroupingPort explicitRelation) {
        this.exactTuple = Objects.requireNonNull(exactTuple, "exactTuple must not be null");
        this.explicitRelation = Objects.requireNonNull(explicitRelation, "explicitRelation must not be null");
    }

    @Override
    public GroupAssignment assignGroups(Gtin gtin, ProjectionSurface surface, List<SourceRecordHead> sourceHeads,
            List<ResolvedValue> resolvedValues, Optional<CanonicalClassId> resolvedClass) {
        GroupAssignment tuple = exactTuple.assignGroups(gtin, surface, sourceHeads, resolvedValues, resolvedClass);
        GroupAssignment relation =
                explicitRelation.assignGroups(gtin, surface, sourceHeads, resolvedValues, resolvedClass);

        GroupId modelGroupId = tuple.modelGroupId() != null ? tuple.modelGroupId() : relation.modelGroupId();

        LinkedHashSet<GroupId> families = new LinkedHashSet<>(tuple.familyGroupIds());
        families.addAll(relation.familyGroupIds());

        LinkedHashSet<String> tokens = new LinkedHashSet<>(tuple.searchTokens());
        tokens.addAll(relation.searchTokens());

        return new GroupAssignment(modelGroupId, List.copyOf(families), List.copyOf(tokens));
    }
}
