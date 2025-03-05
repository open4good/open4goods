package org.open4goods.services.reviewgeneration.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the review generation service.
 */
@Configuration
@ConfigurationProperties(prefix = "review.generation")
public class ReviewGenerationProperties {

    /**
     * The size of the thread pool used for processing review generation tasks.
     */
    private int threadPoolSize = 10;

    /**
     * Maximum number of characters to remove from the model name during retry.
     */
    private int maxCharactersToRemove = 4;

    /**
     * Number of search results to fetch from Google.
     */
    private int numberOfResults = 10;

    /**
     * List of preferred domains to be present in search results.
     */
    private List<String> preferredDomains = new ArrayList<String>();

    // Getters and setters

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
}
