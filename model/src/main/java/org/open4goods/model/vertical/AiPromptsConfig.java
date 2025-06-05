package org.open4goods.model.vertical;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record AiPromptsConfig(boolean override, String rootPrompt, List<LegacyPromptConfig> prompts) {

        public AiPromptsConfig() {
                this(false, null, new ArrayList<>());
        }
	
	
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
        public List<LegacyPromptConfig> getPrompts() {
                return prompts;
        }
        public boolean isOverride() {
                return override;
        }

	
}
