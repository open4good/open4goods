
package org.open4goods.config.yml.attributes;

/**
 *
 * @author goulven
 *
 */
public class PromptConfig {

	/**
	 * The identifier for this attribute.
	 */
	private String key;


	private String prompt;

	/**
	 * Rate limit delay in milliseconds.
	 */
	private int rateLimitDelay;
	
	
	/**
	 * If true, texts will be regenerated
	 */
	private boolean override = false;


	public String getKey() {
		return key;
	}


	public void setKey(String key) {
		this.key = key;
	}


	public String getPrompt() {
		return prompt;
	}


	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}


	public boolean isOverride() {
		return override;
	}


	public void setOverride(boolean override) {
		this.override = override;
	}

	public int getRateLimitDelay() {
		return rateLimitDelay;
	}

	public void setRateLimitDelay(int rateLimitDelay) {
		this.rateLimitDelay = rateLimitDelay;
	}
}
