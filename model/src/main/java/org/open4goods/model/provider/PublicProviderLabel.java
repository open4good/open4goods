package org.open4goods.model.provider;

/**
 * One reviewed, public-facing provider label for an internal source id.
 *
 * @param sourceId internal source id (the aggregated price's raw
 *     {@code datasourceName}), never exposed to a caller
 * @param label approved public label shown in place of the internal source id
 * @param faviconUrl approved public favicon URL for this provider, or
 *     {@code null} when none is reviewed
 */
public record PublicProviderLabel(String sourceId, String label, String faviconUrl) {

    public PublicProviderLabel {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("sourceId must not be blank");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
    }
}
