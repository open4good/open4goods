package org.open4goods.nudgerfrontapi.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties steering the share resolution workflow.
 */
@ConfigurationProperties(prefix = "front.share")
public class ShareResolutionProperties {

    private boolean crawlerExtractionEnabled = false;
    private Duration resolutionWindow = Duration.ofSeconds(4);
    private Duration storeTtl = Duration.ofMinutes(20);
    private int maxUrlLength = 2048;
    private int maxTextLength = 2048;
    private int maxTitleLength = 256;
    private int maxCandidates = 5;

    public boolean isCrawlerExtractionEnabled() {
        return crawlerExtractionEnabled;
    }

    public void setCrawlerExtractionEnabled(boolean crawlerExtractionEnabled) {
        this.crawlerExtractionEnabled = crawlerExtractionEnabled;
    }

    public Duration getResolutionWindow() {
        return resolutionWindow;
    }

    public void setResolutionWindow(Duration resolutionWindow) {
        this.resolutionWindow = resolutionWindow;
    }

    public Duration getStoreTtl() {
        return storeTtl;
    }

    public void setStoreTtl(Duration storeTtl) {
        this.storeTtl = storeTtl;
    }

    public int getMaxUrlLength() {
        return maxUrlLength;
    }

    public void setMaxUrlLength(int maxUrlLength) {
        this.maxUrlLength = maxUrlLength;
    }

    public int getMaxTextLength() {
        return maxTextLength;
    }

    public void setMaxTextLength(int maxTextLength) {
        this.maxTextLength = maxTextLength;
    }

    public int getMaxTitleLength() {
        return maxTitleLength;
    }

    public void setMaxTitleLength(int maxTitleLength) {
        this.maxTitleLength = maxTitleLength;
    }

    public int getMaxCandidates() {
        return maxCandidates;
    }

    public void setMaxCandidates(int maxCandidates) {
        this.maxCandidates = maxCandidates;
    }
}
