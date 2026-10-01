package org.open4goods.datareference.model.projection;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.model.grouping.GroupId;
import org.open4goods.datareference.model.grouping.GroupType;

/**
 * Queryable model/family grouping carried on a GTIN leaf.
 *
 * <p>ADR-0010: GTIN remains the leaf identity, and a family is never returned
 * as a product. {@code modelGroupId} and {@code familyGroupIds} only ever hold
 * CONFIRMED membership -- an unreviewed common-prefix candidate is never
 * assigned here, regardless of how it is surfaced through a candidate-search
 * query port.
 *
 * @param modelGroupId confirmed model group, or absent when brand, O4G class or
 *     full model is missing or does not match an explicit relation or the exact
 *     normalized tuple
 * @param familyGroupIds confirmed family groups, ordered and without duplicates;
 *     empty when none apply
 * @param searchTokens normalized, deduplicated model tokens used for prefix
 *     candidate discovery; independent of whether a group was confirmed
 */
public record GroupAssignment(GroupId modelGroupId, List<GroupId> familyGroupIds, List<String> searchTokens) {

    /** The empty assignment: no confirmed group, no search tokens. */
    public static final GroupAssignment NONE = new GroupAssignment(null, List.of(), List.of());

    /** Validates group types and deduplicates search tokens and family ids. */
    public GroupAssignment {
        if (modelGroupId != null && modelGroupId.type() != GroupType.MODEL) {
            throw new IllegalArgumentException("modelGroupId must have type MODEL: " + modelGroupId);
        }
        Objects.requireNonNull(familyGroupIds, "familyGroupIds must not be null");
        LinkedHashSet<GroupId> dedupedFamilies = new LinkedHashSet<>();
        for (GroupId familyGroupId : familyGroupIds) {
            Objects.requireNonNull(familyGroupId, "familyGroupIds must not contain null");
            if (familyGroupId.type() != GroupType.FAMILY) {
                throw new IllegalArgumentException("familyGroupIds must have type FAMILY: " + familyGroupId);
            }
            dedupedFamilies.add(familyGroupId);
        }
        familyGroupIds = List.copyOf(dedupedFamilies);
        Objects.requireNonNull(searchTokens, "searchTokens must not be null");
        LinkedHashSet<String> dedupedTokens = new LinkedHashSet<>();
        for (String token : searchTokens) {
            Objects.requireNonNull(token, "searchTokens must not contain null");
            if (token.isBlank()) {
                throw new IllegalArgumentException("searchTokens must not contain blanks");
            }
            dedupedTokens.add(token);
        }
        searchTokens = List.copyOf(dedupedTokens);
    }

    /**
     * Returns the confirmed model group, when one was assigned.
     *
     * @return the model group id, or empty
     */
    public Optional<GroupId> modelGroup() {
        return Optional.ofNullable(modelGroupId);
    }
}
