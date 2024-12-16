package org.open4goods.commons.config.yml.ui;

import org.open4goods.commons.model.Localisable;

import com.fasterxml.jackson.annotation.JsonMerge;

public class ImpactScoreCriteria {

	private String key;
	
	@JsonMerge
	private Localisable<String, String> description = new Localisable<>();

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Localisable<String, String> getDescription() {
		return description;
	}

	public void setDescription(Localisable<String, String> description) {
		this.description = description;
	}
	
	
}
