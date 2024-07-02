package org.open4goods.config.yml.attributes;

import java.util.ArrayList;
import java.util.List;

public class AiPromptsConfig {

	private String rootPrompt;
	List<PromptConfig> prompts = new ArrayList<>();
	public String getRootPrompt() {
		return rootPrompt;
	}
	public void setRootPrompt(String rootPrompt) {
		this.rootPrompt = rootPrompt;
	}
	public List<PromptConfig> getPrompts() {
		return prompts;
	}
	public void setPrompts(List<PromptConfig> configs) {
		this.prompts = configs;
	}
	
	
	
}
