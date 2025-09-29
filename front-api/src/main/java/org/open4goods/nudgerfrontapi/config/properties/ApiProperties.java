package org.open4goods.nudgerfrontapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for front cache settings.
 */
@ConfigurationProperties(prefix = "front")
public class ApiProperties {

    /** The resource provider root path (where static resources will be hosted) */
    private String resourceProviderRootPath;

    public String getResourceProviderRootPath() {
        return resourceProviderRootPath;
    }

    public void setResourceProviderRootPath(String resourceProviderRootPath) {
        this.resourceProviderRootPath = resourceProviderRootPath;
    }

}
