package org.open4goods.services.prompt.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Represents a prompt configuration which defines the AI service to use,
 * templated prompts, and associated options for the chat model.
 */
public class PromptConfig {

    /**
     * The unique key used to identify this prompt configuration.
     */
    private String key;

    /**
     * The generative AI service to use.
     */
    private GenAiServiceType aiService;

    /**
     * The system prompt template.
     */
    private String systemPrompt;

    /**
     * The user prompt template.
     */
    private String userPrompt;

    /**
     * Retrieval mode for the prompt.
     */
    private RetrievalMode retrievalMode = RetrievalMode.EXTERNAL_SOURCES;

    /**
     * Provider-agnostic options for the chat model (e.g., temperature, max tokens).
     */
    private PromptOptions options = new PromptOptions();

    /**
     * Provider-specific options that are not mapped to PromptOptions.
     */
    private Map<String, Object> providerOptions = new HashMap<>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public GenAiServiceType getAiService() {
        return aiService;
    }

    public void setAiService(GenAiServiceType aiService) {
        this.aiService = aiService;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }

    public RetrievalMode getRetrievalMode() {
        return retrievalMode == null ? RetrievalMode.EXTERNAL_SOURCES : retrievalMode;
    }

    public void setRetrievalMode(RetrievalMode retrievalMode) {
        this.retrievalMode = retrievalMode;
    }

    public PromptOptions getOptions() {
        return options;
    }

    public void setOptions(PromptOptions options) {
        this.options = options;
    }

    @JsonSetter("options")
    public void setOptionsFromYaml(Map<String, Object> optionsMap) {
        if (optionsMap == null) {
            this.options = new PromptOptions();
            this.providerOptions = new HashMap<>();
            return;
        }
        this.providerOptions = new HashMap<>();
        this.options = PromptOptions.fromMap(optionsMap, this.providerOptions);
    }

    public Map<String, Object> getProviderOptions() {
        return providerOptions;
    }

    public void setProviderOptions(Map<String, Object> providerOptions) {
        this.providerOptions = providerOptions;
    }

    @Override
    public String toString() {
        return "PromptConfig{" +
                "key='" + key + '\'' +
                ", aiService=" + aiService +
                ", systemPrompt='" + systemPrompt + '\'' +
                ", userPrompt='" + userPrompt + '\'' +
                ", retrievalMode=" + retrievalMode +
                ", options=" + options +
                ", providerOptions=" + providerOptions +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, aiService, systemPrompt, userPrompt, retrievalMode, options, providerOptions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PromptConfig)) return false;
        PromptConfig that = (PromptConfig) o;
        return Objects.equals(key, that.key) &&
               aiService == that.aiService &&
               Objects.equals(systemPrompt, that.systemPrompt) &&
               Objects.equals(userPrompt, that.userPrompt) &&
               retrievalMode == that.retrievalMode &&
               Objects.equals(options, that.options) &&
               Objects.equals(providerOptions, that.providerOptions);
    }
}
