package org.open4goods.ui.config.yml;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Holds external configuration for the URL check service,
 * including the list of "bad patterns" to detect,
 * the size of the thread pool,
 * and the main sitemap URL to fetch.
 */
@Configuration
@ConfigurationProperties(prefix = "urlcheck")
public class UrlCheckConfig {

    /**
     * Main sitemap URL to read from. E.g.:
     *   urlcheck.sitemap-url: "https://www.example.com/sitemap_index.xml"
     */
    private String sitemapUrl;

    /**
     * A list of bad patterns, read from application.yml
     * Example:
     *   urlcheck.bad-patterns:
     *     - "Internal Server Error"
     *     - "database error"
     *     - "Fatal error"
     */
    private List<String> badPatterns = new ArrayList<>();

    /**
     * The size of the thread pool used by the service.
     */
    private int threadPoolSize = 5;


    // ---- Getters & Setters ----

    public String getSitemapUrl() {
        return sitemapUrl;
    }

    public void setSitemapUrl(String sitemapUrl) {
        this.sitemapUrl = sitemapUrl;
    }

    public List<String> getBadPatterns() {
        return badPatterns;
    }

    public void setBadPatterns(List<String> badPatterns) {
        this.badPatterns = badPatterns;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }
}
