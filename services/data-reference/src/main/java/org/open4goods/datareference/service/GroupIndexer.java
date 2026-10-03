package org.open4goods.datareference.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.grouping.GroupConfirmation;
import org.open4goods.datareference.model.grouping.GroupId;
import org.open4goods.datareference.model.grouping.GroupIndexEntry;
import org.open4goods.datareference.model.grouping.GroupIndexResult;
import org.open4goods.datareference.model.grouping.GroupLineageEvent;
import org.open4goods.datareference.model.grouping.GroupMember;

/**
 * Builds and maintains the compact group index (GOU-199) from confirmed
 * {@link GroupMember} memberships.
 *
 * <p>Every aggregation here is purely a function of the member set and sorts
 * by {@link Gtin#value()} before sampling or picking a representative field,
 * so the same registry, rule and input versions always produce the same
 * index regardless of arrival order: {@link #index} is idempotent, and
 * {@link #reindex} detects the same splits whether members are replayed in
 * their original order or not.
 */
public final class GroupIndexer {

    private static final Comparator<Gtin> BY_VALUE = Comparator.comparing(Gtin::value);
    private static final Comparator<GroupMember> BY_GTIN = Comparator.comparing(GroupMember::gtin, BY_VALUE);

    /**
     * Result of folding predecessor entries into one canonical entry.
     *
     * @param entry the merged, canonical entry
     * @param event audit record of the merge
     */
    public record MergeResult(GroupIndexEntry entry, GroupLineageEvent.Merge event) {
    }

    /**
     * Builds a fresh compact index from confirmed memberships.
     *
     * @param members every GTIN leaf's confirmed membership, for one surface
     * @param evidenceVersion rule or registry version that produced these memberships
     * @return one entry per group id carrying at least one member
     */
    public List<GroupIndexEntry> index(List<GroupMember> members, RuleVersion evidenceVersion) {
        Objects.requireNonNull(members, "members must not be null");
        Objects.requireNonNull(evidenceVersion, "evidenceVersion must not be null");

        Map<GroupId, List<GroupMember>> byGroup = new LinkedHashMap<>();
        for (GroupMember member : members) {
            byGroup.computeIfAbsent(member.groupId(), id -> new ArrayList<>()).add(member);
        }

        List<GroupIndexEntry> entries = new ArrayList<>();
        for (Map.Entry<GroupId, List<GroupMember>> group : byGroup.entrySet()) {
            entries.add(buildEntry(group.getKey(), group.getValue(), evidenceVersion));
        }
        entries.sort(Comparator.comparing(entry -> entry.groupId().externalForm()));
        return List.copyOf(entries);
    }

    private GroupIndexEntry buildEntry(GroupId groupId, List<GroupMember> groupMembers, RuleVersion evidenceVersion) {
        List<GroupMember> sorted = groupMembers.stream().sorted(BY_GTIN).toList();

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        LinkedHashSet<String> searchTokens = new LinkedHashSet<>();
        for (GroupMember member : sorted) {
            member.label().ifPresent(labels::add);
            searchTokens.addAll(member.searchTokens());
        }

        Optional<String> canonicalBrand = sorted.stream()
                .map(GroupMember::canonicalBrand)
                .flatMap(Optional::stream)
                .findFirst();
        Optional<CanonicalClassId> canonicalClass = sorted.stream()
                .map(GroupMember::canonicalClass)
                .flatMap(Optional::stream)
                .findFirst();

        List<Gtin> representativeGtins = sorted.stream()
                .map(GroupMember::gtin)
                .distinct()
                .limit(GroupIndexEntry.MAX_REPRESENTATIVE_GTINS)
                .toList();

        long memberCount = sorted.stream().map(GroupMember::gtin).distinct().count();

        return new GroupIndexEntry(groupId, GroupConfirmation.CONFIRMED, List.copyOf(labels),
                List.copyOf(searchTokens), canonicalBrand, canonicalClass, evidenceVersion,
                Math.toIntExact(memberCount), representativeGtins, List.of());
    }

    /**
     * Rebuilds the index and detects the splits caused by the membership change.
     *
     * <p>A previous group id that no longer carries any member is a split only
     * when its former members now resolve under two or more different group
     * ids; a previous group id whose members moved together under a single new
     * id is a rename, not a split, and leaves no lineage event here -- callers
     * that need renames tracked supply a stable {@link GroupId} slug instead.
     *
     * @param previousMembers membership before the change, empty on a first build
     * @param currentMembers membership after the change
     * @param evidenceVersion rule or registry version that produced {@code currentMembers}
     * @return the rebuilt index and any split events detected against the previous membership
     */
    public GroupIndexResult reindex(List<GroupMember> previousMembers, List<GroupMember> currentMembers,
            RuleVersion evidenceVersion) {
        Objects.requireNonNull(previousMembers, "previousMembers must not be null");
        List<GroupIndexEntry> entries = index(currentMembers, evidenceVersion);

        Map<Gtin, GroupId> currentAssignment = currentMembers.stream()
                .collect(Collectors.toMap(GroupMember::gtin, GroupMember::groupId, (a, b) -> a));
        Set<GroupId> currentGroupIds =
                currentMembers.stream().map(GroupMember::groupId).collect(Collectors.toSet());

        Map<GroupId, List<Gtin>> previousByGroup = new LinkedHashMap<>();
        for (GroupMember member : previousMembers) {
            previousByGroup.computeIfAbsent(member.groupId(), id -> new ArrayList<>()).add(member.gtin());
        }

        List<GroupLineageEvent> lineage = new ArrayList<>();
        Instant occurredAt = Instant.now();
        for (Map.Entry<GroupId, List<Gtin>> previousGroup : previousByGroup.entrySet()) {
            GroupId previousGroupId = previousGroup.getKey();
            if (currentGroupIds.contains(previousGroupId)) {
                continue;
            }
            Set<GroupId> destinations = previousGroup.getValue().stream()
                    .map(currentAssignment::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (destinations.size() >= 2) {
                lineage.add(new GroupLineageEvent.Split(previousGroupId, List.copyOf(destinations), evidenceVersion,
                        occurredAt));
            }
        }
        return new GroupIndexResult(entries, List.copyOf(lineage));
    }

    /**
     * Folds predecessor entries into one canonical entry (GOU-199: a rule-overlap
     * merge decision).
     *
     * @param toMerge entries being reconciled, including the canonical one; at least two
     * @param canonicalGroupId id that survives, must be one of {@code toMerge}'s ids
     * @param evidenceVersion rule or registry version that confirmed the merge
     * @param occurredAt instant the merge is recorded
     * @return the merged entry and its audit event
     */
    public MergeResult merge(List<GroupIndexEntry> toMerge, GroupId canonicalGroupId, RuleVersion evidenceVersion,
            Instant occurredAt) {
        Objects.requireNonNull(toMerge, "toMerge must not be null");
        Objects.requireNonNull(canonicalGroupId, "canonicalGroupId must not be null");
        Objects.requireNonNull(evidenceVersion, "evidenceVersion must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (toMerge.size() < 2) {
            throw new IllegalArgumentException("merge requires at least two entries, the canonical one included");
        }

        GroupIndexEntry canonical = toMerge.stream()
                .filter(entry -> entry.groupId().equals(canonicalGroupId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "canonicalGroupId must be one of the merged entries: " + canonicalGroupId));

        List<GroupIndexEntry> predecessors =
                toMerge.stream().filter(entry -> !entry.groupId().equals(canonicalGroupId)).toList();

        for (GroupIndexEntry entry : toMerge) {
            if (entry.confirmation() != GroupConfirmation.CONFIRMED) {
                throw new IllegalArgumentException("merge requires every entry to be CONFIRMED: " + entry.groupId());
            }
            if (entry.groupId().type() != canonicalGroupId.type()) {
                throw new IllegalArgumentException(
                        "merge requires every entry to share the canonical group's type: " + entry.groupId());
            }
        }

        LinkedHashSet<String> labels = new LinkedHashSet<>(canonical.labels());
        predecessors.forEach(entry -> labels.addAll(entry.labels()));

        LinkedHashSet<String> searchTokens = new LinkedHashSet<>(canonical.searchTokens());
        predecessors.forEach(entry -> searchTokens.addAll(entry.searchTokens()));

        Optional<String> canonicalBrand = canonical.canonicalBrand()
                .or(() -> predecessors.stream().map(GroupIndexEntry::canonicalBrand).flatMap(Optional::stream)
                        .findFirst());
        Optional<CanonicalClassId> canonicalClass = canonical.canonicalClass()
                .or(() -> predecessors.stream().map(GroupIndexEntry::canonicalClass).flatMap(Optional::stream)
                        .findFirst());

        int memberCount = toMerge.stream().mapToInt(GroupIndexEntry::memberCount).sum();

        List<Gtin> representativeGtins = toMerge.stream()
                .flatMap(entry -> entry.representativeGtins().stream())
                .distinct()
                .sorted(BY_VALUE)
                .limit(GroupIndexEntry.MAX_REPRESENTATIVE_GTINS)
                .toList();

        LinkedHashSet<GroupId> predecessorAliases = new LinkedHashSet<>(canonical.predecessorAliases());
        for (GroupIndexEntry predecessor : predecessors) {
            predecessorAliases.add(predecessor.groupId());
            predecessorAliases.addAll(predecessor.predecessorAliases());
        }

        GroupIndexEntry merged = new GroupIndexEntry(canonicalGroupId, GroupConfirmation.CONFIRMED,
                List.copyOf(labels), List.copyOf(searchTokens), canonicalBrand, canonicalClass, evidenceVersion,
                memberCount, representativeGtins, List.copyOf(predecessorAliases));

        GroupLineageEvent.Merge event = new GroupLineageEvent.Merge(canonicalGroupId,
                predecessors.stream().map(GroupIndexEntry::groupId).toList(), evidenceVersion, occurredAt);

        return new MergeResult(merged, event);
    }
}
