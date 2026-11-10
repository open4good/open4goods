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
}
