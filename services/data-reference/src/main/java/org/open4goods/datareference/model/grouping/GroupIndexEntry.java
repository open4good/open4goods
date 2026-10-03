package org.open4goods.datareference.model.grouping;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.RuleVersion;

/**
 * Compact, queryable projection of one group, never an unbounded member list.
 *
 * <p>GOU-199: a group can have an unbounded number of GTIN leaves, but the
 * index only ever carries a count and a capped sample of
 * {@link #representativeGtins()}. Anything that needs the full membership
 * reads it from the per-GTIN {@code GroupAssignment} on the product
 * projection, not from this index.
 *
 * <p>An entry is computed for one {@link org.open4goods.datareference.model.ProjectionSurface}
 * at a time, from that surface's own {@code GroupAssignment}s: resolution is
 * already surface-scoped upstream (a brand, model or explicit relation can be
 * withheld from a surface by {@code SourceUsagePolicy} before grouping ever
 * runs), so an entry built from one surface's memberships never carries
 * another surface's forbidden evidence. A query port keeps one index per
 * surface rather than one shared index redacted after the fact.
 *
 * @param groupId stable identifier
 * @param confirmation whether this entry is backed by actual membership or is
 *     an unreviewed candidate
 * @param labels human-readable labels for this group, deduplicated, ordered
 * @param searchTokens normalized tokens the union of this group's members
 *     carry, used for prefix candidate discovery
 * @param canonicalBrand normalized brand this group is scoped to, when known
 * @param canonicalClass O4G class this group is scoped to, when known
 * @param evidenceVersion rule or registry version that produced this entry
 * @param memberCount total number of GTIN leaves carrying this group,
 *     independently of how many are sampled in {@link #representativeGtins()}
 * @param representativeGtins capped, deterministic sample of member GTINs
 * @param predecessorAliases group ids a merge folded into this one; empty
 *     unless this entry is the canonical survivor of a merge
 */
public record GroupIndexEntry(
        GroupId groupId,
        GroupConfirmation confirmation,
        List<String> labels,
        List<String> searchTokens,
        Optional<String> canonicalBrand,
        Optional<CanonicalClassId> canonicalClass,
        RuleVersion evidenceVersion,
        int memberCount,
        List<Gtin> representativeGtins,
        List<GroupId> predecessorAliases) {

    /** Maximum number of GTINs ever carried by {@link #representativeGtins()}. */
    public static final int MAX_REPRESENTATIVE_GTINS = 5;

    /** Validates field invariants and defensively copies collections. */
    public GroupIndexEntry {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(confirmation, "confirmation must not be null");
        Objects.requireNonNull(evidenceVersion, "evidenceVersion must not be null");
        Objects.requireNonNull(canonicalBrand, "canonicalBrand must not be null");
        Objects.requireNonNull(canonicalClass, "canonicalClass must not be null");

        labels = dedupe(labels, "labels");
        searchTokens = dedupe(searchTokens, "searchTokens");

        Objects.requireNonNull(representativeGtins, "representativeGtins must not be null");
        LinkedHashSet<Gtin> dedupedGtins = new LinkedHashSet<>();
        for (Gtin gtin : representativeGtins) {
            dedupedGtins.add(Objects.requireNonNull(gtin, "representativeGtins must not contain null"));
        }
        if (dedupedGtins.size() > MAX_REPRESENTATIVE_GTINS) {
            throw new IllegalArgumentException(
                    "representativeGtins must not exceed " + MAX_REPRESENTATIVE_GTINS + " entries: "
                            + dedupedGtins.size());
        }
        representativeGtins = List.copyOf(dedupedGtins);

        if (memberCount < representativeGtins.size()) {
            throw new IllegalArgumentException(
                    "memberCount must be at least the number of representativeGtins: " + memberCount + " < "
                            + representativeGtins.size());
        }

        Objects.requireNonNull(predecessorAliases, "predecessorAliases must not be null");
        LinkedHashSet<GroupId> dedupedAliases = new LinkedHashSet<>();
        for (GroupId alias : predecessorAliases) {
            Objects.requireNonNull(alias, "predecessorAliases must not contain null");
            if (alias.equals(groupId)) {
                throw new IllegalArgumentException("predecessorAliases must not contain the entry's own groupId");
            }
            if (alias.type() != groupId.type()) {
                throw new IllegalArgumentException(
                        "predecessorAliases must share the entry's group type: " + alias + " vs " + groupId.type());
            }
            dedupedAliases.add(alias);
        }
        predecessorAliases = List.copyOf(dedupedAliases);
    }

    private static List<String> dedupe(List<String> values, String fieldName) {
        Objects.requireNonNull(values, fieldName + " must not be null");
        LinkedHashSet<String> deduped = new LinkedHashSet<>();
        for (String value : values) {
            Objects.requireNonNull(value, fieldName + " must not contain null");
            if (value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " must not contain blanks");
            }
            deduped.add(value);
        }
        return List.copyOf(deduped);
    }
}
