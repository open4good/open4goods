package org.open4goods.model.ai;

import java.util.Date;

/**
 * Simple holder for AI generated text along with its creation timestamp.
 */
public record AiDescription(Date ts, String content) {

    /**
     * Convenience constructor initializing the timestamp to the current time.
     *
     * @param content the generated text
     */
    public AiDescription(String content) {
        this(new Date(), content);
    }

    /**
     * Accessor preserving the former {@code getTs()} API.
     *
     * @return the timestamp of creation
     */
    public Date getTs() {
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
