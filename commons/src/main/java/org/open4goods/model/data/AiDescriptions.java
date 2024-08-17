package org.open4goods.model.data;

import java.util.Map;

import org.open4goods.config.yml.attributes.PromptConfig;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class AiDescriptions {
	
	/**
	 * Descriptions, keyed by the identifying key (matching {@link PromptConfig.key}
	 */
	@Field(index = false, store = false, type = FieldType.Object)
	private Map<String,AiDescription> descriptions;
	
	
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
