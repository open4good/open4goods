package org.open4goods.model.vertical;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AiPromptsConfig {

	/**
	 * If true, texts will be regenerated
	 */
	private boolean override = false;
	
	/**
	 * The prompt injected before the other prompts
	 */
	private String rootPrompt;
	
	/**
	 * List of specific prompts
	 */
	List<LegacyPromptConfig> prompts = new ArrayList<>();
	
	
	/**
	 * Shortcut to retrieve the prompts keys
	 * @return
	 */
	public Set<String> promptKeys() {
		return prompts.stream().map(e-> e.getKey()).collect(Collectors.toSet());
	}
	
	public String getRootPrompt() {
		return rootPrompt;
	}
	public void setRootPrompt(String rootPrompt) {
		this.rootPrompt = rootPrompt;
	}
	public List<LegacyPromptConfig> getPrompts() {
		return prompts;
	}
	public void setPrompts(List<LegacyPromptConfig> configs) {
		this.prompts = configs;
	}
	public boolean isOverride() {
		return override;
	}
	public void setOverride(boolean override) {
		this.override = override;
	}

	
}
