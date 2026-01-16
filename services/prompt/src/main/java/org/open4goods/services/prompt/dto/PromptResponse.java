package org.open4goods.services.prompt.dto;

import org.open4goods.services.prompt.config.PromptConfig;
import java.util.Objects;

/**
 * Data transfer object representing the response of a prompt execution.
 *
 * @param <T> the type of the response body
 */
public class PromptResponse<T> {

    /**
     * The body of the response.
     */
    private T body;

    /**
     * The resolved prompt configuration with variables evaluated.
     */
    private PromptConfig prompt = new PromptConfig();

    /**
     * The raw response content.
     */
    private String raw;

    /**
     * Provider metadata associated with the response.
     */
    private java.util.Map<String, Object> metadata = new java.util.HashMap<>();

    /**
     * Duration of the generation process (in milliseconds).
     */
    private long duration;

    /**
     * Timestamp (epoch ms) when the generation occurred.
     */
    private long start;

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    public PromptConfig getPrompt() {
        return prompt;
    }

    public void setPrompt(PromptConfig prompt) {
        this.prompt = prompt;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public java.util.Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(java.util.Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "PromptResponse{" +
                "body=" + body +
                ", prompt=" + prompt +
                ", raw='" + raw + '\'' +
                ", metadata=" + metadata +
                ", duration=" + duration +
                ", start=" + start +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(body, prompt, raw, metadata, duration, start);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PromptResponse)) return false;
        PromptResponse<?> that = (PromptResponse<?>) o;
        return duration == that.duration &&
               start == that.start &&
               Objects.equals(body, that.body) &&
               Objects.equals(prompt, that.prompt) &&
               Objects.equals(raw, that.raw) &&
               Objects.equals(metadata, that.metadata);
    }
}
