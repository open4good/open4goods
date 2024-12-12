package org.open4goods.commons.config.yml;

import org.springframework.ai.openai.OpenAiChatOptions;

/**
 * Represents a prompt config (a chat model configuration and a chat model options) 
 */
public class PromptConfig {
	
	/**
	 * The unique key used to identify this prompt config
	 */
	private String key;
	
	/**
	 * The Gen ai service to use
	 */
	private GenAiServiceType aiService;
	
	/**
	 * The system prompt
	 */
	private String systemPrompt;
	
	
	/**
	 * The user prompt
	 */
	private String userPrompt;
	
	
	/**
	 * The options (temperature, top k..)given to the chat model
	 */
	private OpenAiChatOptions options;
	

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
	

	
	
}
