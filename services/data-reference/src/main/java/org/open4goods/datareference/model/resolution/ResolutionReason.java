package org.open4goods.datareference.model.resolution;

/**
 * Why one candidate won.
 *
 * <p>Stored with every resolved value: a projection that says what won without
 * saying why cannot be argued with, and a support question about a wrong value
 * then costs a full replay to answer.
 */
public enum ResolutionReason {
    /** A reviewed O4G correction applied in its declared scope. */
    O4G_CORRECTION,
    /** The concept rule names this source as authoritative. */
    SOURCE_AUTHORITY,
    /** The candidate carried the highest attachment confidence. */
    HIGHER_CONFIDENCE,
    /** The candidate was observed most recently. */
    MORE_RECENT_OBSERVATION,
    /** Candidates were otherwise equal; the stable id order decided. */
    STABLE_TIE_BREAK,
    /** A named concept rule derived the value from several candidates. */
    NAMED_DERIVATION_RULE
}
