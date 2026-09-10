package org.open4goods.datareference.model;

import java.net.URI;

/**
 * Attribution content to publish with an allowed source projection.
 *
 * @param required whether attribution is mandatory
 * @param notice attribution notice, or {@code null} when not required
 * @param sourceUri source link, or {@code null} when not required
 */
public record AttributionRequirement(boolean required, String notice, URI sourceUri) {

    /** Attribution contract for a source that requires no notice. */
    public static final AttributionRequirement NONE = new AttributionRequirement(false, null, null);

    /**
     * Ensures mandatory attribution has both text and a source URI.
     */
    public AttributionRequirement {
        if (required && (notice == null || notice.isBlank() || sourceUri == null)) {
            throw new IllegalArgumentException("required attribution needs a notice and source URI");
        }
        if (!required && (notice != null || sourceUri != null)) {
            throw new IllegalArgumentException("optional attribution must use the NONE contract");
        }
    }
}
