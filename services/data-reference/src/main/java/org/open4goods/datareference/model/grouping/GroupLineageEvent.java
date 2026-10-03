package org.open4goods.datareference.model.grouping;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import org.open4goods.datareference.model.RuleVersion;

/**
 * Audit link left behind when the group index merges or splits group ids.
 *
 * <p>GOU-199: a merge keeps one canonical id and aliases the predecessors on
 * {@link GroupIndexEntry#predecessorAliases()}; a split allocates new ids and
 * leaves the member GTINs under them. Neither mutation is reversible from the
 * compact index alone, so the event is the durable record of what happened,
 * independently of whatever the index looks like after later reindexing.
 */
public sealed interface GroupLineageEvent {

    /** Group type every id named by this event shares. */
    GroupType type();

    /** Instant the lineage change was recorded. */
    Instant occurredAt();

    /** Rule or registry version responsible for the change. */
    RuleVersion evidenceVersion();

    /**
     * Two or more previously distinct groups were folded into one canonical id.
     *
     * @param canonicalGroupId id that survives the merge
     * @param mergedGroupIds predecessor ids folded into the canonical one, never
     *     including {@code canonicalGroupId} itself
     * @param evidenceVersion rule or registry version that confirmed the merge
     * @param occurredAt instant the merge was recorded
     */
    record Merge(GroupId canonicalGroupId, List<GroupId> mergedGroupIds, RuleVersion evidenceVersion,
            Instant occurredAt) implements GroupLineageEvent {

        /** Validates type consistency and non-emptiness. */
        public Merge {
            Objects.requireNonNull(canonicalGroupId, "canonicalGroupId must not be null");
            Objects.requireNonNull(evidenceVersion, "evidenceVersion must not be null");
            Objects.requireNonNull(occurredAt, "occurredAt must not be null");
            Objects.requireNonNull(mergedGroupIds, "mergedGroupIds must not be null");
            LinkedHashSet<GroupId> deduped = new LinkedHashSet<>();
            for (GroupId mergedGroupId : mergedGroupIds) {
                Objects.requireNonNull(mergedGroupId, "mergedGroupIds must not contain null");
                if (mergedGroupId.equals(canonicalGroupId)) {
                    throw new IllegalArgumentException("mergedGroupIds must not contain canonicalGroupId");
                }
                if (mergedGroupId.type() != canonicalGroupId.type()) {
                    throw new IllegalArgumentException(
                            "mergedGroupIds must share canonicalGroupId's type: " + mergedGroupId);
                }
                deduped.add(mergedGroupId);
            }
            if (deduped.isEmpty()) {
                throw new IllegalArgumentException("a merge must fold in at least one predecessor group id");
            }
            mergedGroupIds = List.copyOf(deduped);
        }

        @Override
        public GroupType type() {
            return canonicalGroupId.type();
        }
    }

    /**
     * One group id was replaced by two or more new, narrower ids.
     *
     * @param originGroupId id that no longer receives new members after the split
     * @param splitGroupIds new ids members of {@code originGroupId} were
     *     reassigned to, never including {@code originGroupId} itself
     * @param evidenceVersion rule or registry version that produced the split
     * @param occurredAt instant the split was recorded
     */
    record Split(GroupId originGroupId, List<GroupId> splitGroupIds, RuleVersion evidenceVersion,
            Instant occurredAt) implements GroupLineageEvent {

        /** Validates type consistency and non-emptiness. */
        public Split {
            Objects.requireNonNull(originGroupId, "originGroupId must not be null");
            Objects.requireNonNull(evidenceVersion, "evidenceVersion must not be null");
            Objects.requireNonNull(occurredAt, "occurredAt must not be null");
            Objects.requireNonNull(splitGroupIds, "splitGroupIds must not be null");
            LinkedHashSet<GroupId> deduped = new LinkedHashSet<>();
            for (GroupId splitGroupId : splitGroupIds) {
                Objects.requireNonNull(splitGroupId, "splitGroupIds must not contain null");
                if (splitGroupId.equals(originGroupId)) {
                    throw new IllegalArgumentException("splitGroupIds must not contain originGroupId");
                }
                if (splitGroupId.type() != originGroupId.type()) {
                    throw new IllegalArgumentException(
                            "splitGroupIds must share originGroupId's type: " + splitGroupId);
                }
                deduped.add(splitGroupId);
            }
            if (deduped.size() < 2) {
                throw new IllegalArgumentException("a split must allocate at least two new group ids");
            }
            splitGroupIds = List.copyOf(deduped);
        }

        @Override
        public GroupType type() {
            return originGroupId.type();
        }
    }
}
