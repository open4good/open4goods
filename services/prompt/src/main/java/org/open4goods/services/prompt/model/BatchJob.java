package org.open4goods.services.prompt.model;

import java.time.Instant;

import org.open4goods.services.prompt.config.GenAiServiceType;

/**
 * Internal representation of a batch job persisted on disk.
 */
public class BatchJob {

    private String id;
    private GenAiServiceType provider;
    private String remoteJobId;
    private BatchJobStatus status;
    private String promptKey;
    private String submissionFilePath;
    private String outputFilePath;
    private String errorMessage;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public GenAiServiceType getProvider() {
        return provider;
    }

    public void setProvider(GenAiServiceType provider) {
        this.provider = provider;
    }

    public String getRemoteJobId() {
        return remoteJobId;
    }

    public void setRemoteJobId(String remoteJobId) {
        this.remoteJobId = remoteJobId;
    }

    public BatchJobStatus getStatus() {
        return status;
    }

    public void setStatus(BatchJobStatus status) {
        this.status = status;
    }

    public String getPromptKey() {
        return promptKey;
    }

    public void setPromptKey(String promptKey) {
        this.promptKey = promptKey;
    }

    public String getSubmissionFilePath() {
        return submissionFilePath;
    }

    public void setSubmissionFilePath(String submissionFilePath) {
        this.submissionFilePath = submissionFilePath;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
