package org.open4goods.nudgerfrontapi.config.properties;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rules used to count the distinct merchant domains that have an active offer (GOU-2037).
 * <p>
 * The backend base URL and API key are the ones of {@code front.affiliation-partners}.
 * </p>
 */
@ConfigurationProperties("front.merchant-domains")
public class MerchantDomainsProperties {

    /**
     * Path appended to {@code front.affiliation-partners.api-base-url} to read the last offer date of each
     * datasource.
     */
    private String activityPath = "/merchants/offer-activity";

    /**
     * Domains of affiliation networks: a datasource resolved to one of them (or to one of their subdomains) is not
     * a merchant and is never counted.
     */
    private Set<String> excludedDomains = new LinkedHashSet<>(List.of(
            "awin.com", "awin1.com", "zanox.com",
            "tradetracker.com", "tradetracker.net",
            "effiliation.com", "effinity.fr",
            "metaffiliation.com", "netaffiliation.com",
            "kwanko.com",
            "cj.com", "dpbolvw.net", "anrdoezrs.net", "jdoqocy.com", "kqzyfj.com", "tkqlhce.com",
            "webgains.com",
            "affilae.com", "timeone.io", "rakutenadvertising.com", "linksynergy.com"));

    /**
     * Merchant domain of a datasource whose affiliation partner has no portal URL and whose name is not a domain
     * (for instance {@code Carrefour - marketplace}). Takes precedence over the portal URL.
     */
    private List<DatasourceDomain> datasourceDomains = new ArrayList<>();

    public String getActivityPath() {
        return activityPath;
    }

    public void setActivityPath(String activityPath) {
        this.activityPath = activityPath;
    }

    public Set<String> getExcludedDomains() {
        return excludedDomains;
    }

    public void setExcludedDomains(Set<String> excludedDomains) {
        this.excludedDomains = excludedDomains;
    }

    public List<DatasourceDomain> getDatasourceDomains() {
        return datasourceDomains;
    }

    public void setDatasourceDomains(List<DatasourceDomain> datasourceDomains) {
        this.datasourceDomains = datasourceDomains;
    }

    /**
     * Merchant domain forced for one datasource.
     */
    public static class DatasourceDomain {

        /** Datasource name, as in the offers (compared trimmed, case insensitive). */
        private String datasource;

        /** Merchant domain or URL of the datasource. */
        private String domain;

        public String getDatasource() {
            return datasource;
        }

        public void setDatasource(String datasource) {
            this.datasource = datasource;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }
    }
}
