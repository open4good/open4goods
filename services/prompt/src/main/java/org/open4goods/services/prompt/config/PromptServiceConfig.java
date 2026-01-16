package org.open4goods.services.prompt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotBlank;

@Component
@ConfigurationProperties(prefix = "gen-ai-config")
public class PromptServiceConfig {
	
	
	  /** Maximum total tokens per batch file (to avoid exceeding limits). */
    private int batchMaxTokens = 200_000_000;  // default 200k

    /** Folder path where batch files (input/output) and job IDs will be stored. */
    @NotBlank
    private String batchFolder = "/opt/open4goods/.cached/batch-ia/";

    /** The OpenAI Batch API endpoint. */
    private String batchApiEndpoint = "https://api.openai.com/v1/batches";
    
	/**
	 * The folder to the yaml prompt files 
	 */
	private String promptsTemplatesFolder;
	
	private boolean cacheTemplates = false;
	
	private String openaiApiKey;

	private String perplexityApiKey;

	private String geminiApiKey;
	
	private String vertexProjectId;
	
	private String vertexLocation = "us-central1";
	
	private String vertexApiKey;
	
	private String vertexCredentialsJson;
	
	private String perplexityBaseUrl = "https://api.perplexity.ai";

	private String perplexityCompletionsPath = "/chat/completions";
	
	private boolean enabled = false;
	
	/**
	 * Flag to enable recording of prompt responses as mocks.
	 */
	private boolean recordEnabled = false;
	
	/**
	 * Folder path where recorded prompt responses (mocks) are stored.
	 */
	private String recordFolder;

	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public String getPromptsTemplatesFolder() {
		return promptsTemplatesFolder;
	}
	public void setPromptsTemplatesFolder(String promptsTemplatesFoler) {
		this.promptsTemplatesFolder = promptsTemplatesFoler;
	}
	public String getOpenaiApiKey() {
		return openaiApiKey;
	}
	public void setOpenaiApiKey(String openaiApiKey) {
		this.openaiApiKey = openaiApiKey;
	}
	public String getPerplexityApiKey() {
		return perplexityApiKey;
	}
	public void setPerplexityApiKey(String perplexityApiKey) {
		this.perplexityApiKey = perplexityApiKey;
	}
	public String getGeminiApiKey() {
		return geminiApiKey;
	}
	public void setGeminiApiKey(String geminiApiKey) {
		this.geminiApiKey = geminiApiKey;
	}
	public String getPerplexityBaseUrl() {
		return perplexityBaseUrl;
	}
	public void setPerplexityBaseUrl(String perplexityBaseUrl) {
		this.perplexityBaseUrl = perplexityBaseUrl;
	}
	public String getPerplexityCompletionsPath() {
		return perplexityCompletionsPath;
	}
	public void setPerplexityCompletionsPath(String perplexityCompletionsPath) {
		this.perplexityCompletionsPath = perplexityCompletionsPath;
	}
	public boolean isCacheTemplates() {
		return cacheTemplates;
	}
	public void setCacheTemplates(boolean cacheTemplates) {
		this.cacheTemplates = cacheTemplates;
	}
	
	public boolean isRecordEnabled() {
	    return recordEnabled;
	}
	
	public void setRecordEnabled(boolean recordEnabled) {
	    this.recordEnabled = recordEnabled;
	}
	
	public String getRecordFolder() {
	    return recordFolder;
	}
	
	public void setRecordFolder(String recordFolder) {
	    this.recordFolder = recordFolder;
	}
	public int getBatchMaxTokens() {
		return batchMaxTokens;
	}
	public void setBatchMaxTokens(int batchMaxTokens) {
		this.batchMaxTokens = batchMaxTokens;
	}
	public String getBatchFolder() {
		return batchFolder;
	}
	public void setBatchFolder(String batchFolder) {
		this.batchFolder = batchFolder;
	}
	public String getBatchApiEndpoint() {
		return batchApiEndpoint;
	}
	public void setBatchApiEndpoint(String batchApiEndpoint) {
		this.batchApiEndpoint = batchApiEndpoint;
	}
	public String getVertexProjectId() {
		return vertexProjectId;
	}
	public void setVertexProjectId(String vertexProjectId) {
		this.vertexProjectId = vertexProjectId;
	}
	public String getVertexLocation() {
		return vertexLocation;
	}
	public void setVertexLocation(String vertexLocation) {
		this.vertexLocation = vertexLocation;
	}
	public String getVertexApiKey() {
		return vertexApiKey;
	}
	public void setVertexApiKey(String vertexApiKey) {
		this.vertexApiKey = vertexApiKey;
	}
	
	
}
