package org.open4goods.services.reviewgeneration.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for DataForSEO Standard Google organic SERP discovery.
 */
@Configuration
@ConfigurationProperties(prefix = "dataforseo.serp")
public class DataForSeoSerpConfig {

    private String username;
    private String password;
    private String baseUrl = "https://api.dataforseo.com";
    private String languageCode = "fr";
    private String locationName = "France";
    private String seDomain = "google.fr";
    private String device = "desktop";
    private int priority = 1;
    private int depth = 10;
    private int maxStoredUrls = 20;
    private int maxTasksPerPost = 100;
    private Duration requestTimeout = Duration.ofSeconds(30);
    private Duration discoveryPollInterval = Duration.ofMinutes(10);
    private String defaultQueryTemplate = "%s \"%s\" (\"fiche technique\" OR caractéristiques OR test OR guide OR comparatif)";

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getSeDomain() {
        return seDomain;
    }

    public void setSeDomain(String seDomain) {
        this.seDomain = seDomain;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getMaxStoredUrls() {
        return maxStoredUrls;
    }

    public void setMaxStoredUrls(int maxStoredUrls) {
        this.maxStoredUrls = maxStoredUrls;
    }

    public int getMaxTasksPerPost() {
        return maxTasksPerPost;
    }

    public void setMaxTasksPerPost(int maxTasksPerPost) {
        this.maxTasksPerPost = maxTasksPerPost;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Duration getDiscoveryPollInterval() {
        return discoveryPollInterval;
    }

    public void setDiscoveryPollInterval(Duration discoveryPollInterval) {
        this.discoveryPollInterval = discoveryPollInterval;
    }

    public String getDefaultQueryTemplate() {
        return defaultQueryTemplate;
    }

    public void setDefaultQueryTemplate(String defaultQueryTemplate) {
        this.defaultQueryTemplate = defaultQueryTemplate;
    }
}
