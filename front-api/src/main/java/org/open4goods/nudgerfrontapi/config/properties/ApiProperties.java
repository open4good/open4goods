package org.open4goods.nudgerfrontapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for front API settings.
 */
@ConfigurationProperties(prefix = "front")
public class ApiProperties {

    /**
     * Absolute root path used to expose downloadable assets. (where static
     * resources will be hosted)
     */
    private String resourceRootPath;

    /**
     * Maximum page size allowed for paginated endpoints. Prevents excessive data
     * retrieval.
     */
    private Integer maxPageSize = 50;

    /**
     * Enable semantic score diagnostics in global search responses.
     */
    private boolean semanticDiagnosticsEnabled;

    public String getResourceRootPath() {
        return resourceRootPath;
    }

    public void setResourceRootPath(String resourceRootPath) {
        this.resourceRootPath = resourceRootPath;
    }

    public Integer getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(Integer maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public boolean isSemanticDiagnosticsEnabled() {
        return semanticDiagnosticsEnabled;
    }

    public void setSemanticDiagnosticsEnabled(boolean semanticDiagnosticsEnabled) {
        this.semanticDiagnosticsEnabled = semanticDiagnosticsEnabled;
    }

}
