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
     * Elasticsearch {@code index.max_result_window}: deep pagination requires
     * {@code (pageNumber + 1) * pageSize <= maxResultWindow}.
     */
    private int maxResultWindow = 10_000;

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

    public int getMaxResultWindow() {
        return maxResultWindow;
    }

    public void setMaxResultWindow(int maxResultWindow) {
        this.maxResultWindow = maxResultWindow;
    }

    /**
     * Tells whether a page lies inside the Elasticsearch result window.
     *
     * @param pageNumber zero-based page index
     * @param pageSize   page size
     * @return {@code true} when {@code (pageNumber + 1) * pageSize <= maxResultWindow}
     */
    public boolean isWithinResultWindow(long pageNumber, long pageSize) {
        return pageNumber >= 0 && pageSize >= 0 && (pageNumber + 1) * pageSize <= maxResultWindow;
    }

    /**
     * Number of pages reachable for a page size without leaving the result window.
     *
     * @param pageSize page size
     * @return reachable page count, {@code Long.MAX_VALUE} when the size is not positive
     */
    public long maxReachablePages(long pageSize) {
        return pageSize <= 0 ? Long.MAX_VALUE : maxResultWindow / pageSize;
    }

}
