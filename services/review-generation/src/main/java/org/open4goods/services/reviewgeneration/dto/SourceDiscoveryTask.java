package org.open4goods.services.reviewgeneration.dto;

/**
 * One DataForSEO task tracked locally for URL discovery.
 */
public class SourceDiscoveryTask {

    private Long productId;
    private String gtin;
    private String query;
    private String taskId;
    private boolean completed;
    private String error;

    public SourceDiscoveryTask() {
    }

    public SourceDiscoveryTask(Long productId, String gtin, String query) {
        this.productId = productId;
        this.gtin = gtin;
        this.query = query;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getGtin() {
        return gtin;
    }

    public void setGtin(String gtin) {
        this.gtin = gtin;
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

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
