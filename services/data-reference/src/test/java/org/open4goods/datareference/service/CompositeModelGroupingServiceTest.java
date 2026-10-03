package org.open4goods.datareference.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.grouping.GroupId;
import org.open4goods.datareference.model.grouping.GroupType;
import org.open4goods.datareference.model.projection.GroupAssignment;
import org.open4goods.datareference.model.resolution.ResolvedValue;
import org.open4goods.datareference.port.ModelGroupingPort;

/**
 * Tests GOU-49 AC2/AC8 composition: either the exact-tuple or the explicit
 * relation path can confirm a MODEL group, neither silently loses what the
 * other confirmed, and the composed result does not depend on which path is
 * invoked first.
 */
class CompositeModelGroupingServiceTest {

    private static final Gtin GTIN = new Gtin("4006381333931");
    private static final GroupId TUPLE_MODEL = new GroupId(GroupType.MODEL, "television-acme-xr-500");
    private static final GroupId RELATION_MODEL = new GroupId(GroupType.MODEL, "4006381333931-5901234123457");
    private static final GroupId TUPLE_FAMILY = new GroupId(GroupType.FAMILY, "acme-tv-family");
    private static final GroupId RELATION_FAMILY = new GroupId(GroupType.FAMILY, "4006381333931-9501234123451");

    @Test
    void bothPathsConfirmingAModelGroupKeepsTheExactTupleOne() {
        ModelGroupingPort tuple = constant(new GroupAssignment(TUPLE_MODEL, List.of(), List.of("acme", "xr", "500")));
        ModelGroupingPort relation = constant(new GroupAssignment(RELATION_MODEL, List.of(), List.of()));

        GroupAssignment composed = new CompositeModelGroupingService(tuple, relation)
                .assignGroups(GTIN, ProjectionSurface.NUDGER_WEB, List.of(), List.of(), Optional.empty());

        assertThat(composed.modelGroup()).contains(TUPLE_MODEL);
    }

    @Test
    void onlyTheRelationPathConfirmingAModelGroupIsStillUsed() {
        ModelGroupingPort tuple = constant(GroupAssignment.NONE);
        ModelGroupingPort relation = constant(new GroupAssignment(RELATION_MODEL, List.of(), List.of()));

        GroupAssignment composed = new CompositeModelGroupingService(tuple, relation)
                .assignGroups(GTIN, ProjectionSurface.NUDGER_WEB, List.of(), List.of(), Optional.empty());

        assertThat(composed.modelGroup()).contains(RELATION_MODEL);
    }

    @Test
    void familyGroupsAndSearchTokensAreTheUnionOfBothPaths() {
        ModelGroupingPort tuple = constant(
                new GroupAssignment(TUPLE_MODEL, List.of(TUPLE_FAMILY), List.of("acme", "xr-500")));
        ModelGroupingPort relation = constant(new GroupAssignment(null, List.of(RELATION_FAMILY), List.of("acme")));

        GroupAssignment composed = new CompositeModelGroupingService(tuple, relation)
                .assignGroups(GTIN, ProjectionSurface.NUDGER_WEB, List.of(), List.of(), Optional.empty());

        assertThat(composed.familyGroupIds()).containsExactlyInAnyOrder(TUPLE_FAMILY, RELATION_FAMILY);
        assertThat(composed.searchTokens()).containsExactlyInAnyOrder("acme", "xr-500");
    }

    @Test
    void repeatedComposedCallsAreStableRegardlessOfWhichPathIsEvaluatedFirstInternally() {
        RecordingPort tuple = new RecordingPort(new GroupAssignment(TUPLE_MODEL, List.of(), List.of()));
        RecordingPort relation = new RecordingPort(new GroupAssignment(RELATION_MODEL, List.of(), List.of()));
        CompositeModelGroupingService composite = new CompositeModelGroupingService(tuple, relation);

        GroupAssignment first = composite.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB, List.of(), List.of(),
                Optional.empty());
        GroupAssignment second = composite.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB, List.of(), List.of(),
                Optional.empty());

        assertThat(tuple.callCount).isEqualTo(2);
        assertThat(relation.callCount).isEqualTo(2);
        assertThat(first.modelGroup()).isEqualTo(second.modelGroup()).contains(TUPLE_MODEL);
    }

    private static final class RecordingPort implements ModelGroupingPort {
        private final GroupAssignment assignment;
        private int callCount;

        RecordingPort(GroupAssignment assignment) {
            this.assignment = assignment;
        }

        @Override
        public GroupAssignment assignGroups(Gtin gtin, ProjectionSurface surface,
                List<SourceRecordHead> sourceHeads, List<ResolvedValue> resolvedValues,
                Optional<CanonicalClassId> resolvedClass) {
            callCount++;
            return assignment;
        }
    }

    private static ModelGroupingPort constant(GroupAssignment assignment) {
        return (gtin, surface, sourceHeads, resolvedValues, resolvedClass) -> assignment;
    }
}
