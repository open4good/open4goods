package org.open4goods.nudgerfrontapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-network switches of the click reference written into the affiliation redirect (GOU-2522).
 * <p>
 * When a network is enabled, {@code /contrib/<token>} writes the id of the {@code AffiliationClick} document into the
 * network's publisher reference parameter, so that a sale reported by the network can be joined to its click. Every
 * switch defaults to {@code false}: the redirect URL is then exactly the one carried by the token. A network is
 * enabled by a separate change, after a test sale has shown the reference in the network's report.
 * </p>
 */
@ConfigurationProperties(prefix = "front.affiliation-click-ref")
public class AffiliationClickRefProperties {

    /** Write the click id into {@code clickref} on Awin links. */
    private boolean awinEnabled;

    /** Write the click id into {@code r} (reference) on TradeTracker links. */
    private boolean tradetrackerEnabled;

    /** Write the click id into {@code effi_id} on Effiliation links. */
    private boolean effiliationEnabled;

    /** Write the click id into {@code argsite} on Kwanko {@code mclic} links. */
    private boolean kwankoEnabled;

    public boolean isAwinEnabled() {
        return awinEnabled;
    }

    public void setAwinEnabled(boolean awinEnabled) {
        this.awinEnabled = awinEnabled;
    }

    public boolean isTradetrackerEnabled() {
        return tradetrackerEnabled;
    }

    public void setTradetrackerEnabled(boolean tradetrackerEnabled) {
        this.tradetrackerEnabled = tradetrackerEnabled;
    }

    public boolean isEffiliationEnabled() {
        return effiliationEnabled;
    }

    public void setEffiliationEnabled(boolean effiliationEnabled) {
        this.effiliationEnabled = effiliationEnabled;
    }

    public boolean isKwankoEnabled() {
        return kwankoEnabled;
    }

    public void setKwankoEnabled(boolean kwankoEnabled) {
        this.kwankoEnabled = kwankoEnabled;
    }
}
