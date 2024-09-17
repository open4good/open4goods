package org.open4goods.commons.model.data;

import java.util.HashMap;
import java.util.Map;

import org.open4goods.commons.config.yml.attributes.PromptConfig;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class AiDescriptions {
	
	/**
	 * Descriptions, keyed by the identifying key (matching {@link PromptConfig.key}
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
