package org.open4goods.nudgerfrontapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for front cache settings.
 */
@Component
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
