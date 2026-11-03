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
 */
public record AffiliationClickStats(Map<String, Long> clicksShort, Map<String, Long> clicksLong, long totalShort,
        long totalLong, List<MerchantClicks> topMerchantsLong) {

    /**
     * Clicks of one merchant through one network.
     *
     * @param merchant datasource name
     * @param network  {@link AffiliationNetwork#key()}
     * @param clicks   number of clicks
     */
    public record MerchantClicks(String merchant, String network, long clicks) {
    }
}
