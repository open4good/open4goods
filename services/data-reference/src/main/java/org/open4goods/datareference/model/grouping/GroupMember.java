package org.open4goods.datareference.model.grouping;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.Gtin;

/**
 * One GTIN leaf's confirmed membership of a group, as fed to {@link
 * org.open4goods.datareference.service.GroupIndexer}.
 *
 * <p>Carries the per-GTIN metadata the compact {@link GroupIndexEntry} needs
 * to aggregate (label, search tokens, canonical brand/class) without the
 * indexer having to re-resolve anything: {@code GroupIndexer} only
 * aggregates, it never derives.
 *
 * @param gtin the member's product identity
 * @param groupId group this GTIN confirms membership of
 * @param label human-readable label contributed by this member, when one applies
 * @param searchTokens this member's normalized model tokens, mirroring
 *     {@link org.open4goods.datareference.model.projection.GroupAssignment#searchTokens()}
 * @param canonicalBrand normalized brand this member resolved, when known
 * @param canonicalClass O4G class this member resolved, when known
 */
public record GroupMember(
        Gtin gtin,
        GroupId groupId,
        Optional<String> label,
        List<String> searchTokens,
        Optional<String> canonicalBrand,
        Optional<CanonicalClassId> canonicalClass) {

    /** Validates required fields and deduplicates search tokens. */
    public GroupMember {
        Objects.requireNonNull(gtin, "gtin must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(label, "label must not be null");
        Objects.requireNonNull(canonicalBrand, "canonicalBrand must not be null");
        Objects.requireNonNull(canonicalClass, "canonicalClass must not be null");
        Objects.requireNonNull(searchTokens, "searchTokens must not be null");
        LinkedHashSet<String> deduped = new LinkedHashSet<>();
        for (String token : searchTokens) {
            Objects.requireNonNull(token, "searchTokens must not contain null");
            if (token.isBlank()) {
                throw new IllegalArgumentException("searchTokens must not contain blanks");
            }
            deduped.add(token);
        }
        searchTokens = List.copyOf(deduped);
    }
}
