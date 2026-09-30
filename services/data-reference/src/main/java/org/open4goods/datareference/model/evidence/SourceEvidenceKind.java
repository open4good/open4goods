package org.open4goods.datareference.model.evidence;

/**
 * Shape of the evidence an assertion carries.
 *
 * <p>Frozen discriminator: the constant names are part of the serialized
 * contract of {@link SourceEvidence} and of every stored assertion.
 */
public enum SourceEvidenceKind {
    /** A single provider value, optionally with a provider unit. */
    SCALAR,
    /** Prose in a stated language. */
    LOCALIZED_TEXT,
    /** A reference to an image, document or other binary. */
    MEDIA,
    /** Membership in a provider classification. */
    CLASSIFICATION,
    /** A link to another product, model or family. */
    RELATION
}
