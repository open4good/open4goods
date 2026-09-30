package org.open4goods.datareference.model.projection;

/** Reasons a deterministic evaluation must be rebuilt. */
public enum EvaluationRefreshTrigger {
    /** Eligible resolved reference values changed. */
    REFERENCE_CHANGED,
    /** Current offer summary changed. */
    OFFER_CHANGED,
    /** Evaluation rules or their registry dependencies changed. */
    RULE_CHANGED,
    /** The deterministic cohort statistics changed. */
    COHORT_CHANGED
}
