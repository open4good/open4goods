package org.open4goods.services.contribution.model;

import java.util.List;
import java.util.Map;

/**
 * Aggregated outbound affiliation clicks over two windows ending at the same instant.
 *
 * @param clicksShort per {@link AffiliationNetwork#key()}, clicks of the short window; every network is present,
 *                    a network without click counting {@code 0}
 * @param clicksLong  per {@link AffiliationNetwork#key()}, clicks of the long window; every network is present
 * @param totalShort  all clicks of the short window
 * @param totalLong   all clicks of the long window
 * @param topMerchantsLong most clicked (merchant, network) pairs of the long window, most clicked first
 * @param topOriginsLong   most clicked origins of the long window, most clicked first, ties ordered by origin
 */
public record AffiliationClickStats(Map<String, Long> clicksShort, Map<String, Long> clicksLong, long totalShort,
        long totalLong, List<MerchantClicks> topMerchantsLong, List<OriginClicks> topOriginsLong) {

    /**
     * Clicks of one window, per network and in total.
     *
     * @param clicks per {@link AffiliationNetwork#key()}, every network present (0 when it has no click)
     * @param total  all clicks of the window
     */
    public record ClickCounts(Map<String, Long> clicks, long total) {
    }

    /**
     * Clicks of one merchant through one network.
     *
     * @param merchant datasource name
     * @param network  {@link AffiliationNetwork#key()}
     * @param clicks   number of clicks
     */
    public record MerchantClicks(String merchant, String network, long clicks) {
    }

    /**
     * Clicks of one origin: the {@code contentPath} of the click when it has one, otherwise its {@code originPath},
     * otherwise {@value #NO_ORIGIN}.
     *
     * @param origin article or guide path, originating page path, or {@value #NO_ORIGIN}
     * @param clicks number of clicks
     */
    public record OriginClicks(String origin, long clicks) {

        /** Origin of the clicks that carry neither a {@code contentPath} nor an {@code originPath}. */
        public static final String NO_ORIGIN = "(none)";
    }
}
