package org.open4goods.nudgerfrontapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for front cache settings.
 */
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
