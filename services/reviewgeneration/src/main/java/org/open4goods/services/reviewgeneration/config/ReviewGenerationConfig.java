package org.open4goods.services.reviewgeneration.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the review generation service.
 */
@Configuration
@ConfigurationProperties(prefix = "review.generation")
public class ReviewGenerationConfig {

    private int threadPoolSize = 10;
    private int maxCharactersToRemove = 4;
    private int numberOfResults = 10;
    private List<String> preferredDomains = new ArrayList<>();

    // New properties for improved behavior:
    private String queryTemplate = "test %s \"%s\"";
    private int maxSearchCalls = 2;
    private int maxSources = 15;
    private int minMarkdownLength = 150;
    private int maxMarkdownLength = 500000;

    // Getters and setters for existing properties...

    public int getThreadPoolSize() {
        return threadPoolSize;
    }
    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }
    public int getMaxCharactersToRemove() {
        return maxCharactersToRemove;
    }
    public void setMaxCharactersToRemove(int maxCharactersToRemove) {
        this.maxCharactersToRemove = maxCharactersToRemove;
    }
    public int getNumberOfResults() {
        return numberOfResults;
    }
    public void setNumberOfResults(int numberOfResults) {
        this.numberOfResults = numberOfResults;
    }
    public List<String> getPreferredDomains() {
        return preferredDomains;
    }
    public void setPreferredDomains(List<String> preferredDomains) {
        this.preferredDomains = preferredDomains;
    }

    // Getters and setters for new properties:
    public String getQueryTemplate() {
        return queryTemplate;
    }
    public void setQueryTemplate(String queryTemplate) {
        this.queryTemplate = queryTemplate;
    }
    public int getMaxSearchCalls() {
        return maxSearchCalls;
    }
    public void setMaxSearchCalls(int maxSearchCalls) {
        this.maxSearchCalls = maxSearchCalls;
    }
    public int getMaxSources() {
        return maxSources;
    }
    public void setMaxSources(int maxSources) {
        this.maxSources = maxSources;
    }
    public int getMinMarkdownLength() {
        return minMarkdownLength;
    }
    public void setMinMarkdownLength(int minMarkdownLength) {
        this.minMarkdownLength = minMarkdownLength;
    }
    public int getMaxMarkdownLength() {
        return maxMarkdownLength;
    }
    public void setMaxMarkdownLength(int maxMarkdownLength) {
        this.maxMarkdownLength = maxMarkdownLength;
    }
}
