package org.open4goods.nudgerfrontapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for pagination settings.
 */
@Component
@ConfigurationProperties(prefix = "front.pagination")
public class PaginationProperties {

    /**
     * Maximum allowed page size for pageable endpoints.
     */
    private int maxPageSize = 50;

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }
}

