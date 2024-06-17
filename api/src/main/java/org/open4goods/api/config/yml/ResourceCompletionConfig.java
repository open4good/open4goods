package org.open4goods.api.config.yml;

import java.util.ArrayList;
import java.util.List;

public class ResourceCompletionConfig {

	private List<ResourceCompletionUrlTemplate> urlTemplates = new ArrayList<>();

	public List<ResourceCompletionUrlTemplate> getUrlTemplates() {
		return urlTemplates;
	}

	public void setUrlTemplates(List<ResourceCompletionUrlTemplate> urlTemplates) {
		this.urlTemplates = urlTemplates;
	}
	
	
}
