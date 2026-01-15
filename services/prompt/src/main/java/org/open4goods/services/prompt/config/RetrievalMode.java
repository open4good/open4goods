package org.open4goods.services.prompt.config;

/**
 * Retrieval modes for prompt execution.
 */
public enum RetrievalMode {
    /**
     * Use externally fetched sources injected into the prompt.
     */
    EXTERNAL_SOURCES,

    /**
     * Use model-native web search or grounding tools.
     */
    MODEL_WEB_SEARCH
}
