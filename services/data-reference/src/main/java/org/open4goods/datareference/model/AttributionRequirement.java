package org.open4goods.datareference.model;

import java.net.URI;

/**
 * Attribution content to publish with an allowed source projection.
 *
 * <p>{@code asIsDisclaimerRequired} is deny-by-default: {@code null} (a field
 * absent from a policy document) reads as {@code true}, the most restrictive
 * reading, so an unreviewed omission never drops the Fair Use Policy "AS IS"
 * disclaimer a source requires.
 *
 * @param required whether attribution is mandatory
 * @param notice attribution notice, or {@code null} when not required
 * @param sourceUri source link, or {@code null} when not required
 * @param asIsDisclaimerRequired whether an explicit "AS IS" disclaimer must be
 *         published alongside the content
 */
public record AttributionRequirement(boolean required, String notice, URI sourceUri, Boolean asIsDisclaimerRequired) {

    /** Attribution contract for a source that requires no notice and no disclaimer. */
    public static final AttributionRequirement NONE = new AttributionRequirement(false, null, null, false);

    /**
     * Ensures mandatory attribution has both text and a source URI, and resolves
     * an absent disclaimer requirement to its most restrictive reading.
     */
    public AttributionRequirement {
        if (required && (notice == null || notice.isBlank() || sourceUri == null)) {
            throw new IllegalArgumentException("required attribution needs a notice and source URI");
        }
        if (!required && (notice != null || sourceUri != null)) {
            throw new IllegalArgumentException("optional attribution must use the NONE contract");
        }
        asIsDisclaimerRequired = asIsDisclaimerRequired == null ? Boolean.TRUE : asIsDisclaimerRequired;
    }
}
