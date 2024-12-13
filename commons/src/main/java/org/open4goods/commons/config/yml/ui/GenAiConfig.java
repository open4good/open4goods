package org.open4goods.commons.config.yml.ui;

public class GenAiConfig {
	
	/**
	 * The folder to the yaml prompt files 
	 */
	private String promptsTemplatesFolder;
	
	private boolean cacheTemplates = true;
	
	private String openaiApiKey;

	private String perplexityApiKey;
	
	private String perplexityBaseUrl = "https://api.perplexity.ai";

	private String perplexityCompletionsPath = "/chat/completions";
	
	
	
	
	private boolean enabled = false;
	
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

	
	
}
