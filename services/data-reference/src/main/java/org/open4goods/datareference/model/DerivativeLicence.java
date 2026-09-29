package org.open4goods.datareference.model;

/**
 * Licence a derived work must carry, as reviewed source terms require.
 *
 * <p>Absent from a policy, this reads as {@link #NONE}: the most restrictive
 * value, since it permits no derivative work rather than an unreviewed one.
 */
public enum DerivativeLicence {
    /** No derivative work of this source's content is permitted. */
    NONE,
    /**
     * A derivative work must be published as a whole, at no charge, under the
     * same licence as the source (share-alike), as required by Icecat Open
     * Content License v1.4 clause 2(b).
     */
    SHARE_ALIKE
}
