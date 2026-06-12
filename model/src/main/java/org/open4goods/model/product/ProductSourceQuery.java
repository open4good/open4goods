package org.open4goods.model.product;

/**
 * Search query metadata associated with a discovered product source URL.
 */
public class ProductSourceQuery {

    private String query;
    private String taskId;
    private ProductSourceProvider provider = ProductSourceProvider.UNKNOWN;
    private long createdAt;

    public ProductSourceQuery() {
    }

    public ProductSourceQuery(String query, String taskId, ProductSourceProvider provider, long createdAt) {
        this.query = query;
        this.taskId = taskId;
        this.provider = provider == null ? ProductSourceProvider.UNKNOWN : provider;
        this.createdAt = createdAt;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public ProductSourceProvider getProvider() {
        return provider;
    }

    public void setProvider(ProductSourceProvider provider) {
        this.provider = provider == null ? ProductSourceProvider.UNKNOWN : provider;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
