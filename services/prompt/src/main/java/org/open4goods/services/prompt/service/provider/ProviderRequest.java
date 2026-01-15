package org.open4goods.services.prompt.service.provider;

import java.util.Map;

import org.open4goods.services.prompt.config.PromptOptions;
import org.open4goods.services.prompt.config.RetrievalMode;

/**
 * Request information sent to a GenAI provider.
 */
public class ProviderRequest {

    private final String promptKey;
    private final String systemPrompt;
    private final String userPrompt;
    private final PromptOptions options;
    private final RetrievalMode retrievalMode;
    private final String jsonSchema;
    private final boolean allowWebSearch;
    private final Map<String, Object> providerOptions;

    public ProviderRequest(String promptKey, String systemPrompt, String userPrompt, PromptOptions options,
                           RetrievalMode retrievalMode, String jsonSchema, boolean allowWebSearch,
                           Map<String, Object> providerOptions) {
        this.promptKey = promptKey;
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
        this.options = options;
        this.retrievalMode = retrievalMode;
        this.jsonSchema = jsonSchema;
        this.allowWebSearch = allowWebSearch;
        this.providerOptions = providerOptions;
    }

    public String getPromptKey() {
        return promptKey;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public PromptOptions getOptions() {
        return options;
    }

    public RetrievalMode getRetrievalMode() {
        return retrievalMode;
    }

    public String getJsonSchema() {
        return jsonSchema;
    }

    public boolean isAllowWebSearch() {
        return allowWebSearch;
    }

    public Map<String, Object> getProviderOptions() {
        return providerOptions;
    }
}
