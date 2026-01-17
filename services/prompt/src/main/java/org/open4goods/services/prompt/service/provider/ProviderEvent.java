package org.open4goods.services.prompt.service.provider;

import java.time.Instant;
import java.util.Map;

import org.open4goods.services.prompt.config.GenAiServiceType;

/**
 * Streaming event emitted by GenAI providers.
 */
public class ProviderEvent {

    /**
     * Event types for streaming execution.
     */
    public enum Type {
        STARTED,
        SEARCHING,
        TOOL_STATUS,
        STREAM_CHUNK,
        METADATA,
        COMPLETED,
        ERROR
    }

    private final Type type;
    private final GenAiServiceType provider;
    private final String model;
    private final String content;
    private final Map<String, Object> metadata;
    private final String errorMessage;
    private final Instant timestamp;

    private ProviderEvent(Type type, GenAiServiceType provider, String model, String content,
                          Map<String, Object> metadata, String errorMessage, Instant timestamp) {
        this.type = type;
        this.provider = provider;
        this.model = model;
        this.content = content;
        this.metadata = metadata;
        this.errorMessage = errorMessage;
        this.timestamp = timestamp;
    }

    public static ProviderEvent started(GenAiServiceType provider, String model) {
        return new ProviderEvent(Type.STARTED, provider, model, null, Map.of(), null, Instant.now());
    }

    public static ProviderEvent searching(GenAiServiceType provider, String model, String message) {
        return new ProviderEvent(Type.SEARCHING, provider, model, message, Map.of(), null, Instant.now());
    }

    public static ProviderEvent toolStatus(GenAiServiceType provider, String model, String toolName, String status,
                                           Map<String, Object> payload) {
        return new ProviderEvent(Type.TOOL_STATUS, provider, model, null,
                Map.of("tool", toolName, "status", status, "payload", payload), null, Instant.now());
    }

    public static ProviderEvent streamChunk(GenAiServiceType provider, String model, String chunk) {
        return new ProviderEvent(Type.STREAM_CHUNK, provider, model, chunk, Map.of(), null, Instant.now());
    }

    public static ProviderEvent metadata(GenAiServiceType provider, String model, Map<String, Object> metadata) {
        return new ProviderEvent(Type.METADATA, provider, model, null, metadata, null, Instant.now());
    }

    public static ProviderEvent completed(GenAiServiceType provider, String model, String content,
                                          Map<String, Object> metadata) {
        return new ProviderEvent(Type.COMPLETED, provider, model, content, metadata, null, Instant.now());
    }

    public static ProviderEvent error(GenAiServiceType provider, String model, String message) {
        return new ProviderEvent(Type.ERROR, provider, model, null, Map.of(), message, Instant.now());
    }

    public Type getType() {
        return type;
    }

    public GenAiServiceType getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
