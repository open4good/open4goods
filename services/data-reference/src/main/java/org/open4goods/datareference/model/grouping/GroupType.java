package org.open4goods.datareference.model.grouping;

/**
 * Kind of stable group a GTIN leaf can belong to.
 *
 * <p>{@code MODEL} is the exact-identity grouping of ADR-0010: an explicit
 * resolved source relation or the exact normalized tuple of canonical brand,
 * O4G class and full model. {@code FAMILY} is the broader, reviewed grouping
 * (explicit family/series relation or a reviewed {@code ModelPatternRule});
 * it never includes unreviewed common-prefix similarity, which stays a
 * candidate and is never assigned a {@code FAMILY} group.
 */
public enum GroupType {
    MODEL,
    FAMILY
}
