package org.open4goods.nudgerfrontapi.model.stats;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * Dated result of the count of distinct merchant domains that have an active offer (GOU-2037).
 *
 * @param domains           counted merchant domains, each with the datasources resolved to it
 * @param computedAt        when the count was computed
 * @param franceOnly        true only when every counted domain is known to deliver in France; false when the
 *                          delivery country of at least one of them is unknown
 * @param trackedSince      since when the api records the offer dates; a count computed less than the offer max-age
 *                          after it may miss merchants
 * @param unresolved        active datasources without a known merchant domain, not counted
 * @param excluded          active datasources not counted, with the reason (affiliation network, outside France)
 */
public record ActiveMerchantDomains(SortedMap<String, List<String>> domains,
                                    Instant computedAt,
                                    boolean franceOnly,
                                    Instant trackedSince,
                                    List<String> unresolved,
                                    Map<String, String> excluded) {

    /**
     * @return number of distinct merchant domains
     */
    public int count() {
        return domains.size();
    }
}
