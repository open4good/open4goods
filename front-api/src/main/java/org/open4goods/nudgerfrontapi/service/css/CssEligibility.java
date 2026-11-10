package org.open4goods.nudgerfrontapi.service.css;

import java.util.Set;

/**
 * Eligibility of one product to the Google CSS API.
 *
 * @param reasons         failed rules, empty when eligible
 * @param merchantDomains distinct merchant domains of the fresh offers (affiliation networks excluded)
 */
public record CssEligibility(Set<CssExclusionReason> reasons, Set<String> merchantDomains) {

    /**
     * @return true when no rule failed
     */
    public boolean eligible() {
        return reasons.isEmpty();
    }
}
