package org.open4goods.datareference.model;

/**
 * Strength of the evidence attaching a provider record to a GTIN.
 */
public enum GtinMatchConfidence {
    /** The provider record explicitly contains the same validated GTIN. */
    EXACT,
    /** Multiple independent identity fields strongly support the attachment. */
    STRONG,
    /** Heuristic evidence supports the attachment but needs conflict handling. */
    WEAK,
    /** The attachment has not been verified. */
    UNVERIFIED
}
