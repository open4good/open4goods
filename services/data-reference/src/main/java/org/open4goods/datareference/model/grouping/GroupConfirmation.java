package org.open4goods.datareference.model.grouping;

/**
 * Review state of a group entry returned by a query port.
 *
 * <p>Mirrors the distinction {@link org.open4goods.datareference.model.projection.GroupAssignment}
 * already draws between confirmed membership and unreviewed search tokens:
 * {@code CONFIRMED} is a {@link GroupId} a GTIN leaf actually carries,
 * {@code CANDIDATE} is a prefix-similarity match surfaced for curation and
 * must never be treated as membership.
 */
public enum GroupConfirmation {
    /** Backed by at least one GTIN leaf's confirmed {@code GroupAssignment}. */
    CONFIRMED,
    /** Unreviewed common-prefix match; not a membership claim. */
    CANDIDATE
}
