package org.open4goods.services.prompt.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Vertex batch inference.
 */
@Component
@ConfigurationProperties(prefix = "vertex.batch")
public class VertexBatchConfig {

    /**
     * GCP project id hosting the Vertex AI batch job.
     */
    private String projectId;

    /**
     * GCP location for Vertex AI batch jobs.
     */
    private String location = "us-central1";

    /**
     * GCS bucket used for batch input/output artifacts.
     */
    private String bucket;

    /**
     * Optional output prefix for batch job results.
     */
    private String outputPrefix = "vertex-batch-output";

    /**
     * Poll interval for batch job status checks.
     */
    private Duration pollInterval = Duration.ofSeconds(30);

    /**
     * Service account JSON content for batch job authentication.
     */
    private String credentialsJson;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getOutputPrefix() {
        return outputPrefix;
    }

    public void setOutputPrefix(String outputPrefix) {
        this.outputPrefix = outputPrefix;
    }

    public Duration getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    public String getCredentialsJson() {
        return credentialsJson;
    }

    public void setCredentialsJson(String credentialsJson) {
        this.credentialsJson = credentialsJson;
    }
}
