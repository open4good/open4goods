package org.open4goods.services.reviewgeneration.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Local persisted representation of a source discovery batch.
 */
public class SourceDiscoveryJob {

    private String jobId;
    private String verticalId;
    private SourceDiscoveryJobStatus status = SourceDiscoveryJobStatus.SUBMITTED;
    private long createdAt;
    private long updatedAt;
    private int submittedTasks;
    private int completedTasks;
    private int discoveredUrls;
    private String error;
    private List<SourceDiscoveryTask> tasks = new ArrayList<>();

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getVerticalId() {
        return verticalId;
    }

    public void setVerticalId(String verticalId) {
        this.verticalId = verticalId;
    }

    public SourceDiscoveryJobStatus getStatus() {
        return status;
    }

    public void setStatus(SourceDiscoveryJobStatus status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getSubmittedTasks() {
        return submittedTasks;
    }

    public void setSubmittedTasks(int submittedTasks) {
        this.submittedTasks = submittedTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public int getDiscoveredUrls() {
        return discoveredUrls;
    }

    public void setDiscoveredUrls(int discoveredUrls) {
        this.discoveredUrls = discoveredUrls;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<SourceDiscoveryTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<SourceDiscoveryTask> tasks) {
        this.tasks = tasks == null ? new ArrayList<>() : tasks;
    }
}
