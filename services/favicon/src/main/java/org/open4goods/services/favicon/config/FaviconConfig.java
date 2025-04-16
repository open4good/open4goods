package org.open4goods.services.favicon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Configuration properties for the Favicon Service.
 * <p>
 * This configuration includes a cache folder, domain mappings, URL read timeout, and a fallback URL template.
 * Example configuration in application.yml:
 * <pre>
 * favicon:
 *   cacheFolder: "/path/to/cache"
 *   urlTimeout: 5000
 *   fallbackUrl: "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url={url}&size=64"
 *   domainMapping:
 *     example.com: "http://www.example.com/favicon.ico"
 *     anotherdomain.com: "file:/opt/favicon.ico"
 *     somedomain.com: "classpath:/images/favicon.png"
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "favicon")
public class FaviconConfig {
    /**
     * The folder where favicon files are cached.
     */
    private String cacheFolder = "/opt/open4goods/.cached/";

    /**
     * Mapping of domains to direct favicon resource locations.
     */
    private Map<String, String> domainMapping;

    /**
     * URL read timeout in milliseconds for fetching HTML pages.
     */
    private int urlTimeout = 5000; // default to 5000 ms

    /**
     * The URL template for fallback favicon retrieval.
     * Use {url} as a placeholder for the site URL.
     */
    private String fallbackUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url={url}&size=64";

    public String getCacheFolder() {
        return cacheFolder;
    }
    public void setCacheFolder(String cacheFolder) {
        this.cacheFolder = cacheFolder;
    }
    public Map<String, String> getDomainMapping() {
        return domainMapping;
    }
    public void setDomainMapping(Map<String, String> domainMapping) {
        this.domainMapping = domainMapping;
    }
    public int getUrlTimeout() {
        return urlTimeout;
    }
    public void setUrlTimeout(int urlTimeout) {
        this.urlTimeout = urlTimeout;
    }
    public String getFallbackUrl() {
        return fallbackUrl;
    }
    public void setFallbackUrl(String fallbackUrl) {
        this.fallbackUrl = fallbackUrl;
    }

    @Override
    public String toString() {
        return "FaviconConfig{" +
                "cacheFolder='" + cacheFolder + '\'' +
                ", domainMapping=" + domainMapping +
                ", urlTimeout=" + urlTimeout +
                ", fallbackUrl='" + fallbackUrl + '\'' +
                '}';
    }
}
