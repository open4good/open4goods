package org.open4goods.services.prompt.service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.open4goods.services.prompt.config.VertexBatchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

/**
 * Client for Vertex AI Gemini batch prediction jobs.
 */
public class VertexGeminiBatchClient {

    private static final Logger logger = LoggerFactory.getLogger(VertexGeminiBatchClient.class);

    private final VertexBatchConfig config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public VertexGeminiBatchClient(VertexBatchConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public String uploadInputFile(String jobId, String content) {
        Storage storage = buildStorage();
        String objectName = "vertex-batch-input/" + jobId + ".jsonl";
        BlobId blobId = BlobId.of(config.getBucket(), objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.create(blobInfo, content.getBytes(StandardCharsets.UTF_8));
        return "gs://" + config.getBucket() + "/" + objectName;
    }

    public String submitBatchJob(String displayName, String model, String inputUri) {
        try {
            String outputUri = "gs://" + config.getBucket() + "/" + config.getOutputPrefix() + "/" + displayName;
            Map<String, Object> requestBody = Map.of(
                    "displayName", displayName,
                    "model", model,
                    "inputConfig", Map.of(
                            "instancesFormat", "jsonl",
                            "gcsSource", Map.of("uris", List.of(inputUri))
                    ),
                    "outputConfig", Map.of(
                            "predictionsFormat", "jsonl",
                            "gcsDestination", Map.of("outputUriPrefix", outputUri)
                    )
            );
            String payload = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(batchEndpoint()))
                    .header("Authorization", "Bearer " + accessToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("Vertex batch submission failed with status " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String name = root.path("name").asText();
            if (name == null || name.isBlank()) {
                throw new IllegalStateException("Vertex batch submission returned empty job name.");
            }
            logger.info("Submitted Vertex batch job {}", name);
            return name;
        } catch (Exception e) {
            throw new IllegalStateException("Vertex batch submission failed", e);
        }
    }

    public JsonNode getJobStatus(String remoteJobId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jobEndpoint(remoteJobId)))
                    .header("Authorization", "Bearer " + accessToken())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("Vertex batch status failed with status " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new IllegalStateException("Vertex batch status retrieval failed", e);
        }
    }

    public String downloadBatchOutput(String outputUriPrefix) {
        Storage storage = buildStorage();
        String prefix = outputUriPrefix.replace("gs://" + config.getBucket() + "/", "");
        StringBuilder combined = new StringBuilder();
        for (var blob : storage.list(config.getBucket(), Storage.BlobListOption.prefix(prefix)).iterateAll()) {
            byte[] content = storage.readAllBytes(blob.getBlobId());
            combined.append(new String(content, StandardCharsets.UTF_8));
            if (!combined.isEmpty() && combined.charAt(combined.length() - 1) != '\n') {
                combined.append(System.lineSeparator());
            }
        }
        return combined.toString();
    }

    private Storage buildStorage() {
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(config.getCredentialsJson().getBytes(StandardCharsets.UTF_8)))
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
            return StorageOptions.newBuilder()
                    .setProjectId(config.getProjectId())
                    .setCredentials(credentials)
                    .build()
                    .getService();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Google Cloud Storage client", e);
        }
    }

    private String accessToken() {
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(config.getCredentialsJson().getBytes(StandardCharsets.UTF_8)))
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
            credentials.refreshIfExpired();
            if (credentials.getAccessToken() == null) {
                credentials.refresh();
            }
            return credentials.getAccessToken().getTokenValue();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to obtain Vertex access token", e);
        }
    }

    private String batchEndpoint() {
        return String.format("https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/batchPredictionJobs",
                config.getLocation(), config.getProjectId(), config.getLocation());
    }

    private String jobEndpoint(String remoteJobId) {
        if (remoteJobId.startsWith("projects/")) {
            return "https://" + config.getLocation() + "-aiplatform.googleapis.com/v1/" + remoteJobId;
        }
        return batchEndpoint() + "/" + remoteJobId;
    }
}
