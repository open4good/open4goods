package org.open4goods.services.prompt.config;

import org.springframework.ai.openai.OpenAiChatOptions;
import java.util.Objects;

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
     * Options for the chat model (e.g., temperature, top-k).
     */
    private OpenAiChatOptions options;

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

    public OpenAiChatOptions getOptions() {
        return options;
    }

    public void setOptions(OpenAiChatOptions options) {
        this.options = options;
    }

    @Override
    public String toString() {
        return "PromptConfig{" +
                "key='" + key + '\'' +
                ", aiService=" + aiService +
                ", systemPrompt='" + systemPrompt + '\'' +
                ", userPrompt='" + userPrompt + '\'' +
                ", options=" + options +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, aiService, systemPrompt, userPrompt, options);
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
               Objects.equals(options, that.options);
    }
}
