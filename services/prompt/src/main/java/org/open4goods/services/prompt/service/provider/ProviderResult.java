package org.open4goods.services.prompt.service.provider;

import java.util.Map;

import org.open4goods.services.prompt.config.GenAiServiceType;

/**
 * Provider result with response metadata.
 */
public class ProviderResult {

    private final GenAiServiceType provider;
    private final String model;
    private final String rawResponse;
    private final String content;
    private final Map<String, Object> metadata;

    public ProviderResult(GenAiServiceType provider, String model, String rawResponse, String content,
                          Map<String, Object> metadata) {
        this.provider = provider;
        this.model = model;
        this.rawResponse = rawResponse;
        this.content = content;
        this.metadata = metadata;
    }

    public GenAiServiceType getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
