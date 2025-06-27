package org.open4goods.nudgerfrontapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for front cache settings.
 */
@Component
@ConfigurationProperties(prefix = "front.cache")
public class CacheProperties {

    /** Path used to store cached files. */
    private String path = "./cache";

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
