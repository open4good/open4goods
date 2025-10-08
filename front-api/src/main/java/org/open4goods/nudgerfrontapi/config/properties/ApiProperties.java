package org.open4goods.nudgerfrontapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for front cache settings.
 */
@ConfigurationProperties(prefix = "front")
public class ApiProperties {

    /** Absolute root path used to expose downloadable assets. (where static resources will be hosted) */
    private String resourceRootPath;

    public String getResourceRootPath() {
        return resourceRootPath;
    }

    public void setResourceRootPath(String resourceRootPath) {
        this.resourceRootPath = resourceRootPath;
    }

}
