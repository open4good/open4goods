package org.open4goods.nudgerfrontapi.config.properties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the Google CSS product pages preparation (GOU-2080, dry run only: nothing is sent to Google).
 * <p>
 * Disabled by default: no CSS bean and no endpoint exists until {@code front.css.enabled=true}.
 * </p>
 */
@ConfigurationProperties("front.css")
public class CssProperties {

    /** Enables the CSS dry run (service and internal admin endpoint). */
    private boolean enabled = false;

    /** Directory receiving the NDJSON files (one per vertical) and the report, created when missing. */
    private String outputDirectory = "/tmp/nudger-css-dry-run";

    /** Public base URL of the site, used to build {@code cppLink}, the headline offer link and relative image URLs. */
    private String siteBaseUrl = "https://nudger.fr";

    /** Content language of the produced inputs ({@code contentLanguage}). */
    private String contentLanguage = "fr";

    /** Feed label of the produced inputs ({@code feedLabel}). */
    private String feedLabel = "FR";

    /** Minimum number of distinct merchant domains with a fresh offer for a product to be eligible. */
    private int minMerchantDomains = 2;

    /** Validity of an input from its generation ({@code expirationDate}); {@code freshnessTime} is deprecated. */
    private Duration expiration = Duration.ofDays(3);

    /** Destinations of the inputs ({@code includedDestinations}). */
    private List<String> includedDestinations = new ArrayList<>(List.of("css_organic_experiences"));

    /** Titles (compared trimmed, case insensitive) that are not a real product title. */
    private Set<String> genericTitles = new LinkedHashSet<>(List.of(
            "produit", "product", "article", "sans titre", "untitled", "inconnu", "unknown", "n/a", "null"));

    /** Maximum number of product details sent per input. */
    private int maxProductDetails = 30;

    /** Synchronisation with Google (GOU-2081), disabled by default. */
    private Sync sync = new Sync();

    public Sync getSync() {
        return sync;
    }

    public void setSync(Sync sync) {
        this.sync = sync;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getSiteBaseUrl() {
        return siteBaseUrl;
    }

    public void setSiteBaseUrl(String siteBaseUrl) {
        this.siteBaseUrl = siteBaseUrl;
    }

    public String getContentLanguage() {
        return contentLanguage;
    }

    public void setContentLanguage(String contentLanguage) {
        this.contentLanguage = contentLanguage;
    }

    public String getFeedLabel() {
        return feedLabel;
    }

    public void setFeedLabel(String feedLabel) {
        this.feedLabel = feedLabel;
    }

    public int getMinMerchantDomains() {
        return minMerchantDomains;
    }

    public void setMinMerchantDomains(int minMerchantDomains) {
        this.minMerchantDomains = minMerchantDomains;
    }

    public Duration getExpiration() {
        return expiration;
    }

    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
    }

    public List<String> getIncludedDestinations() {
        return includedDestinations;
    }

    public void setIncludedDestinations(List<String> includedDestinations) {
        this.includedDestinations = includedDestinations;
    }

    public Set<String> getGenericTitles() {
        return genericTitles;
    }

    public void setGenericTitles(Set<String> genericTitles) {
        this.genericTitles = genericTitles;
    }

    public int getMaxProductDetails() {
        return maxProductDetails;
    }

    public void setMaxProductDetails(int maxProductDetails) {
        this.maxProductDetails = maxProductDetails;
    }

    /**
     * Settings of the synchronisation with the Google CSS API ({@code front.css.sync.*}).
     * <p>
     * Needs {@code front.css.enabled=true} as well. With {@code enabled=false} (default) no client, no scheduled job and
     * no credential is needed.
     * </p>
     */
    public static class Sync {

        /** Enables the synchronisation job and the CSS client. */
        private boolean enabled = false;

        /** Uses the simulated client (no network call): dry run of the whole synchronisation. */
        private boolean simulated = false;

        /** CSS account (domain or group) identifier: requests target {@code accounts/<accountId>}. */
        private String accountId;

        /** Path of the service account JSON file (never in the repository). */
        private String serviceAccountPath;

        /** Raw service account JSON, alternative to the path (typically an environment variable). */
        private String serviceAccountJson;

        /** OAuth scope of the CSS API. */
        private String scope = "https://www.googleapis.com/auth/content";

        /** Delay between two runs (fixed delay after the end of a run). */
        private Duration interval = Duration.ofHours(6);

        /** Delay before the first run after startup. */
        private Duration initialDelay = Duration.ofMinutes(5);

        /** State file of the last successful send (JSON), kept across restarts. */
        private String stateFile = "/tmp/nudger-css-sync-state.json";

        /** An unchanged product is sent again when its expiration is closer than this delay. */
        private Duration resendBefore = Duration.ofHours(24);

        /** Maximum number of calls (inserts and deletes) per run, 0 for no limit. */
        private int maxProductsPerRun = 0;

        /** Maximum number of calls per second, 0 for no limit. */
        private double maxRequestsPerSecond = 2.0;

        /** Verticals synchronised, empty for all enabled verticals. */
        private Set<String> verticals = new LinkedHashSet<>();

        /** A run is aborted after this number of consecutive failed calls (state of the sent ones is kept). */
        private int maxConsecutiveFailures = 10;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isSimulated() {
            return simulated;
        }

        public void setSimulated(boolean simulated) {
            this.simulated = simulated;
        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getServiceAccountPath() {
            return serviceAccountPath;
        }

        public void setServiceAccountPath(String serviceAccountPath) {
            this.serviceAccountPath = serviceAccountPath;
        }

        public String getServiceAccountJson() {
            return serviceAccountJson;
        }

        public void setServiceAccountJson(String serviceAccountJson) {
            this.serviceAccountJson = serviceAccountJson;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public Duration getInterval() {
            return interval;
        }

        public void setInterval(Duration interval) {
            this.interval = interval;
        }

        public Duration getInitialDelay() {
            return initialDelay;
        }

        public void setInitialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
        }

        public String getStateFile() {
            return stateFile;
        }

        public void setStateFile(String stateFile) {
            this.stateFile = stateFile;
        }

        public Duration getResendBefore() {
            return resendBefore;
        }

        public void setResendBefore(Duration resendBefore) {
            this.resendBefore = resendBefore;
        }

        public int getMaxProductsPerRun() {
            return maxProductsPerRun;
        }

        public void setMaxProductsPerRun(int maxProductsPerRun) {
            this.maxProductsPerRun = maxProductsPerRun;
        }

        public double getMaxRequestsPerSecond() {
            return maxRequestsPerSecond;
        }

        public void setMaxRequestsPerSecond(double maxRequestsPerSecond) {
            this.maxRequestsPerSecond = maxRequestsPerSecond;
        }

        public Set<String> getVerticals() {
            return verticals;
        }

        public void setVerticals(Set<String> verticals) {
            this.verticals = verticals;
        }

        public int getMaxConsecutiveFailures() {
            return maxConsecutiveFailures;
        }

        public void setMaxConsecutiveFailures(int maxConsecutiveFailures) {
            this.maxConsecutiveFailures = maxConsecutiveFailures;
        }
    }
}
