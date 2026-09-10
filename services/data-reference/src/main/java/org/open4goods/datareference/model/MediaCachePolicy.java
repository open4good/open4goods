package org.open4goods.datareference.model;

import java.time.Duration;

/**
 * Cache limits for source media and other content.
 *
 * @param imageBytesAllowed whether image bytes may be persisted
 * @param imageLinkRetention maximum retention for a provider image link
 * @param nonImageRetention maximum retention for non-image source content
 */
public record MediaCachePolicy(
        boolean imageBytesAllowed,
        Duration imageLinkRetention,
        Duration nonImageRetention) {

    /** Cache policy that permits no retained provider content. */
    public static final MediaCachePolicy NONE = new MediaCachePolicy(false, Duration.ZERO, Duration.ZERO);

    /**
     * Validates non-negative retention periods.
     */
    public MediaCachePolicy {
        if (imageLinkRetention == null || imageLinkRetention.isNegative()
                || nonImageRetention == null || nonImageRetention.isNegative()) {
            throw new IllegalArgumentException("media cache retention must be non-negative");
        }
    }
}
