package org.open4goods.model.ai;

import java.util.HashMap;
import java.util.Map;


public class AiDescriptions {
	
	/**
	 * Descriptions, keyed by the identifying key (matching {@link LegacyPromptConfig.key}
	 */
	private Map<String,AiDescription> descriptions = new HashMap<String, AiDescription>();
	
	
	public AiDescriptions() {
		super();

	}


	public Map<String, AiDescription> getDescriptions() {
		return descriptions;
	}


	public void setDescriptions(Map<String, AiDescription> descriptions) {
		this.descriptions = descriptions;
	}



}
