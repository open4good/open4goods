package org.open4goods.datareference.model;

/**
 * How a provider record came to be attached to a GTIN.
 *
 * <p>Recorded separately from {@link GtinMatchConfidence} because the method is
 * a fact about the attachment procedure while confidence is a judgement about
 * its strength: a reviewed correction and a declared identifier can both be
 * {@code EXACT} yet need to be told apart when a rule changes.
 */
public enum GtinMatchMethod {
    /** The provider record carries the GTIN itself. */
    DECLARED_IDENTIFIER,
    /** A provider identifier resolved to the GTIN through a reviewed mapping. */
    MAPPED_IDENTIFIER,
    /** Brand and normalized model matched exactly. */
    BRAND_AND_MODEL,
    /** Free-text evidence such as an offer title supported the attachment. */
    TEXT_EVIDENCE,
    /** A reviewed O4G correction asserted the attachment. */
    O4G_CORRECTION
}
