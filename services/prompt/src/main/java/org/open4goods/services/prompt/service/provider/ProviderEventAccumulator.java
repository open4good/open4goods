package org.open4goods.services.prompt.service.provider;

import java.util.HashMap;
import java.util.Map;

/**
 * Accumulates streaming provider events into final content and metadata.
 */
public class ProviderEventAccumulator {

    private final StringBuilder contentBuilder = new StringBuilder();
    private final Map<String, Object> metadata = new HashMap<>();

    /**
     * Accepts an incoming provider event and updates the accumulated state.
     *
     * @param event the incoming provider event
     */
    public void accept(ProviderEvent event) {
        if (event == null) {
            return;
        }
        if (event.getType() == ProviderEvent.Type.STREAM_CHUNK && event.getContent() != null) {
            contentBuilder.append(event.getContent());
        }
        if (event.getType() == ProviderEvent.Type.COMPLETED && event.getContent() != null
                && contentBuilder.length() == 0) {
            contentBuilder.append(event.getContent());
        }
        if (event.getMetadata() != null && !event.getMetadata().isEmpty()) {
            metadata.putAll(event.getMetadata());
        }
    }

    /**
     * Returns the accumulated content.
     *
     * @return the accumulated content
     */
    public String getContent() {
        return contentBuilder.toString();
    }

    /**
     * Returns the accumulated metadata.
     *
     * @return the accumulated metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
