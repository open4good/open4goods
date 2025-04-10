package org.open4goods.services.prompt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps the response from the OpenAI Batch API.
 */
public class BatchJobResponse {
    private String id;
    private String object;
    private String status;
    // Additional fields from the API response can be added as needed.

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("object")
    public String getObject() {
        return object;
    }

    @JsonProperty("object")
    public void setObject(String object) {
        this.object = object;
    }

    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }
}
