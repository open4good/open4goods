package org.open4goods.services.prompt.dto;

import org.open4goods.services.prompt.config.PromptConfig;

public class PromptResponse<T> {
	
	/**
	 * The body of the response
	 */
	private T body;
	
	
	/**
	 * The resolved prompt (with variables resolved)
	 */
	private PromptConfig prompt = new PromptConfig();
	
	/**
	 * The response raw content
	 */
	private String raw;
	
	/**
	 * The duration this gen ai task has 
	 */
	private long duration;

	/**
	 * The date this generation occured, in epoch ms 
	 */
	private long start;
	
	
	public T getBody() {
		return body;
	}

	public void setBody(T body) {
		this.body = body;
	}

	public PromptConfig getPrompt() {
		return prompt;
	}

	public void setPrompt(PromptConfig prompt) {
		this.prompt = prompt;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public String getRaw() {
		return raw;
	}

	public void setRaw(String raw) {
		this.raw = raw;
	}
	
	
	

}
