package org.open4goods.services.prompt.dto.openai;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single request entry in a batch submission.
 */
public class BatchRequestEntry {
    private String customId;
    private String method;
    private String url;
    private BatchRequestBody body;

    public String getCustomId() {
        return customId;
    }

    @JsonProperty("custom_id")
    public void setCustomId(String customId) {
        this.customId = customId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public BatchRequestBody getBody() {
        return body;
    }

    public void setBody(BatchRequestBody body) {
        this.body = body;
    }
}
