package org.open4goods.datareference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.grouping.GroupConfirmation;
import org.open4goods.datareference.model.grouping.GroupId;
import org.open4goods.datareference.model.grouping.GroupIndexEntry;
import org.open4goods.datareference.model.grouping.GroupIndexResult;
import org.open4goods.datareference.model.grouping.GroupLineageEvent;
import org.open4goods.datareference.model.grouping.GroupMember;
import org.open4goods.datareference.model.grouping.GroupType;

/**
 * Tests GOU-199 AC5/AC6/AC9: the compact index is a deterministic,
 * idempotent aggregation, reindexing surfaces splits, and a rule-overlap
 * merge keeps one canonical id while aliasing the predecessors.
 */
class GroupIndexerTest {

    private static final RuleVersion VERSION = new RuleVersion("exact-tuple", 1);
    private static final GroupId GROUP_A = new GroupId(GroupType.FAMILY, "acme-tv-xr");
    private static final GroupId GROUP_B = new GroupId(GroupType.FAMILY, "acme-tv-xr-plus");
    private static final GroupId GROUP_C = new GroupId(GroupType.FAMILY, "acme-tv-xr-mini");

    private static GroupMember member(String gtin, GroupId groupId, String label, String... tokens) {
        return new GroupMember(new Gtin(gtin), groupId, Optional.of(label), List.of(tokens),
                Optional.of("acme"), Optional.empty());
    }

    @Test
    void indexAggregatesLabelsSearchTokensAndCountsForOneGroup() {
        List<GroupMember> members = List.of(
                member("1111111111111", GROUP_A, "Acme TV XR", "xr", "500"),
                member("2222222222222", GROUP_A, "Acme TV XR", "xr", "600"));

        List<GroupIndexEntry> entries = new GroupIndexer().index(members, VERSION);

        assertThat(entries).hasSize(1);
        GroupIndexEntry entry = entries.get(0);
        assertThat(entry.groupId()).isEqualTo(GROUP_A);
        assertThat(entry.confirmation()).isEqualTo(GroupConfirmation.CONFIRMED);
        assertThat(entry.labels()).containsExactly("Acme TV XR");
        assertThat(entry.searchTokens()).containsExactly("xr", "500", "600");
        assertThat(entry.memberCount()).isEqualTo(2);
        assertThat(entry.representativeGtins()).containsExactly(new Gtin("1111111111111"), new Gtin("2222222222222"));
        assertThat(entry.canonicalBrand()).contains("acme");
    }

    @Test
    void indexIsIdempotentRegardlessOfMemberArrivalOrder() {
        List<GroupMember> forward = List.of(
                member("3333333333333", GROUP_A, "Acme TV XR", "xr"),
                member("1111111111111", GROUP_A, "Acme TV XR", "xr"),
                member("2222222222222", GROUP_A, "Acme TV XR", "xr"));
        List<GroupMember> reversed = new ArrayList<>(forward);
        java.util.Collections.reverse(reversed);

        List<GroupIndexEntry> fromForward = new GroupIndexer().index(forward, VERSION);
        List<GroupIndexEntry> fromReversed = new GroupIndexer().index(reversed, VERSION);

        assertThat(fromForward).isEqualTo(fromReversed);
    }

    @Test
    void indexCapsRepresentativeGtinsAtTheMaximumAndKeepsTheLowestValuedOnes() {
        List<GroupMember> members = new ArrayList<>();
        for (int i = 9; i >= 1; i--) {
            members.add(member(String.format("%013d", i), GROUP_A, "Acme TV XR", "xr"));
        }

        GroupIndexEntry entry = new GroupIndexer().index(members, VERSION).get(0);

        assertThat(entry.memberCount()).isEqualTo(9);
        assertThat(entry.representativeGtins()).hasSize(GroupIndexEntry.MAX_REPRESENTATIVE_GTINS);
        assertThat(entry.representativeGtins()).containsExactly(
                new Gtin("0000000000001"), new Gtin("0000000000002"), new Gtin("0000000000003"),
                new Gtin("0000000000004"), new Gtin("0000000000005"));
    }

    @Test
    void reindexDetectsASplitWhenAPreviousGroupsMembersMoveToTwoDifferentGroups() {
        List<GroupMember> previous = List.of(
                member("1111111111111", GROUP_A, "Acme TV XR", "xr"),
                member("2222222222222", GROUP_A, "Acme TV XR", "xr"),
                member("3333333333333", GROUP_A, "Acme TV XR", "xr"));
        List<GroupMember> current = List.of(
                member("1111111111111", GROUP_B, "Acme TV XR Plus", "xr", "plus"),
                member("2222222222222", GROUP_C, "Acme TV XR Mini", "xr", "mini"),
                member("3333333333333", GROUP_C, "Acme TV XR Mini", "xr", "mini"));

        GroupIndexResult result = new GroupIndexer().reindex(previous, current, VERSION);

        assertThat(result.entries()).extracting(GroupIndexEntry::groupId).containsExactlyInAnyOrder(GROUP_B, GROUP_C);
        assertThat(result.lineage()).hasSize(1);
        GroupLineageEvent.Split split = (GroupLineageEvent.Split) result.lineage().get(0);
        assertThat(split.originGroupId()).isEqualTo(GROUP_A);
        assertThat(split.splitGroupIds()).containsExactlyInAnyOrder(GROUP_B, GROUP_C);
    }

    @Test
    void reindexDoesNotReportASplitWhenAllMembersMoveTogetherToOneNewId() {
        List<GroupMember> previous = List.of(member("1111111111111", GROUP_A, "Acme TV XR", "xr"));
        List<GroupMember> current = List.of(member("1111111111111", GROUP_B, "Acme TV XR Plus", "xr"));

        GroupIndexResult result = new GroupIndexer().reindex(previous, current, VERSION);

        assertThat(result.lineage()).isEmpty();
    }

    @Test
    void reindexWithUnchangedMembershipIsIdempotentAndReportsNoLineage() {
        List<GroupMember> members = List.of(
                member("1111111111111", GROUP_A, "Acme TV XR", "xr"),
                member("2222222222222", GROUP_A, "Acme TV XR", "xr"));

        GroupIndexResult first = new GroupIndexer().reindex(members, members, VERSION);
        GroupIndexResult second = new GroupIndexer().reindex(members, members, VERSION);

        assertThat(first.lineage()).isEmpty();
        assertThat(second.lineage()).isEmpty();
        assertThat(first.entries()).isEqualTo(second.entries());
    }

    @Test
    void mergeFoldsARuleOverlapPredecessorIntoTheCanonicalIdAndAliasesIt() {
        // Two independently reviewed ModelPatternRules confirmed overlapping family
        // groups over disjoint evidence; a curator reconciles them into one id.
        GroupIndexEntry canonical = new GroupIndexEntry(GROUP_A, GroupConfirmation.CONFIRMED, List.of("Acme TV XR"),
                List.of("xr"), Optional.of("acme"), Optional.empty(), VERSION, 2,
                List.of(new Gtin("1111111111111"), new Gtin("2222222222222")), List.of());
        GroupIndexEntry duplicate = new GroupIndexEntry(GROUP_B, GroupConfirmation.CONFIRMED,
                List.of("Acme TV XR Plus"), List.of("xr", "plus"), Optional.empty(), Optional.empty(), VERSION, 1,
                List.of(new Gtin("3333333333333")), List.of());
        Instant occurredAt = Instant.parse("2026-01-01T00:00:00Z");
        RuleVersion mergeVersion = new RuleVersion("curator-merge", 1);

        GroupIndexer.MergeResult result =
                new GroupIndexer().merge(List.of(canonical, duplicate), GROUP_A, mergeVersion, occurredAt);

        GroupIndexEntry merged = result.entry();
        assertThat(merged.groupId()).isEqualTo(GROUP_A);
        assertThat(merged.memberCount()).isEqualTo(3);
        assertThat(merged.labels()).containsExactlyInAnyOrder("Acme TV XR", "Acme TV XR Plus");
        assertThat(merged.searchTokens()).containsExactlyInAnyOrder("xr", "plus");
        assertThat(merged.canonicalBrand()).contains("acme");
        assertThat(merged.predecessorAliases()).containsExactly(GROUP_B);
        assertThat(merged.representativeGtins()).containsExactly(
                new Gtin("1111111111111"), new Gtin("2222222222222"), new Gtin("3333333333333"));

        assertThat(result.event().canonicalGroupId()).isEqualTo(GROUP_A);
        assertThat(result.event().mergedGroupIds()).containsExactly(GROUP_B);
        assertThat(result.event().evidenceVersion()).isEqualTo(mergeVersion);
        assertThat(result.event().occurredAt()).isEqualTo(occurredAt);
    }

    @Test
    void mergeIsTransitiveOverAPredecessorsOwnPriorAliases() {
        GroupIndexEntry canonical = new GroupIndexEntry(GROUP_A, GroupConfirmation.CONFIRMED, List.of(), List.of(),
                Optional.empty(), Optional.empty(), VERSION, 1, List.of(new Gtin("1111111111111")), List.of());
        GroupIndexEntry alreadyMerged = new GroupIndexEntry(GROUP_B, GroupConfirmation.CONFIRMED, List.of(),
                List.of(), Optional.empty(), Optional.empty(), VERSION, 1, List.of(new Gtin("2222222222222")),
                List.of(GROUP_C));

        GroupIndexer.MergeResult result = new GroupIndexer().merge(List.of(canonical, alreadyMerged), GROUP_A,
                VERSION, Instant.now());

        assertThat(result.entry().predecessorAliases()).containsExactlyInAnyOrder(GROUP_B, GROUP_C);
    }

    @Test
    void mergeRejectsACanonicalIdNotAmongTheMergedEntries() {
        GroupIndexEntry entryA = new GroupIndexEntry(GROUP_A, GroupConfirmation.CONFIRMED, List.of(), List.of(),
                Optional.empty(), Optional.empty(), VERSION, 0, List.of(), List.of());
        GroupIndexEntry entryB = new GroupIndexEntry(GROUP_B, GroupConfirmation.CONFIRMED, List.of(), List.of(),
                Optional.empty(), Optional.empty(), VERSION, 0, List.of(), List.of());

        assertThatThrownBy(() -> new GroupIndexer().merge(List.of(entryA, entryB), GROUP_C, VERSION, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("canonicalGroupId");
    }

    @Test
    void mergeRejectsFewerThanTwoEntries() {
        GroupIndexEntry entryA = new GroupIndexEntry(GROUP_A, GroupConfirmation.CONFIRMED, List.of(), List.of(),
                Optional.empty(), Optional.empty(), VERSION, 0, List.of(), List.of());

        assertThatThrownBy(() -> new GroupIndexer().merge(List.of(entryA), GROUP_A, VERSION, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least two");
    }

    @Test
    void mergeRejectsAnUnconfirmedCandidateEntry() {
        GroupIndexEntry confirmed = new GroupIndexEntry(GROUP_A, GroupConfirmation.CONFIRMED, List.of(), List.of(),
                Optional.empty(), Optional.empty(), VERSION, 0, List.of(), List.of());
        GroupIndexEntry candidate = new GroupIndexEntry(GROUP_B, GroupConfirmation.CANDIDATE, List.of(), List.of(),
                Optional.empty(), Optional.empty(), VERSION, 0, List.of(), List.of());

        assertThatThrownBy(() -> new GroupIndexer().merge(List.of(confirmed, candidate), GROUP_A, VERSION,
                Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CONFIRMED");
    }
}
