package org.open4goods.datareference.model;

/**
 * Review state of a versioned source-usage policy.
 */
public enum PolicyReviewState {
    /** Supporting evidence was inventoried, but an owner has not approved publication. */
    UNREVIEWED,
    /** An owner reviewed this exact policy version and its evidence references. */
    REVIEWED
}
