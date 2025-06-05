package org.open4goods.model.ai;

/**
 * Simple holder for AI generated text along with its creation timestamp.
 */
public record AiDescription(long ts, String content) {

    /**
     * Convenience constructor initializing the timestamp to the current time.
     *
     * @param content the generated text
     */
    public AiDescription(String content) {
        this(System.currentTimeMillis(), content);
    }

    /**
     * Accessor preserving the former {@code getTs()} API.
     *
     * @return the timestamp of creation
     */
    public long getTs() {
        return ts;
    }

    /**
     * Accessor preserving the former {@code getContent()} API.
     *
     * @return the textual content
     */
    public String getContent() {
        return content;
    }
}
