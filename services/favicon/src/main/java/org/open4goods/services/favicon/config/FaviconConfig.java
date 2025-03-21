package org.open4goods.services.favicon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Configuration properties for the Favicon Service.
 * <p>
 * This configuration includes a cache folder and domain mappings.
 * Example configuration in application.yml:
 * <pre>
 * favicon:
 *   cacheFolder: "/path/to/cache"
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
    private String cacheFolder;

    /**
     * Mapping of domains to direct favicon resource locations.
     */
    private Map<String, String> domainMapping;

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

    @Override
    public String toString() {
        return "FaviconConfig{" +
                "cacheFolder='" + cacheFolder + '\'' +
                ", domainMapping=" + domainMapping +
                '}';
    }
}
