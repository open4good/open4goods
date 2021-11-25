package org.open4goods.model.dto;

import java.util.Map;

public class I18nUsage {
	private Map<String, Long> keys;

	public I18nUsage() {

	}

	
	public I18nUsage(Map<String, Long> keys) {
		super();
		this.keys = keys;
	}

	public Map<String, Long> getKeys() {
		return keys;
	}

	public void setKeys(Map<String, Long> keys) {
		this.keys = keys;
	}
	
	
	
}
