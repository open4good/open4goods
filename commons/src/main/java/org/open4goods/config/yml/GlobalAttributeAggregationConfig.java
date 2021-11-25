package org.open4goods.config.yml;

import java.util.HashSet;
import java.util.Set;

public class GlobalAttributeAggregationConfig {
	
	private Set<String> excludedAttributeNames = new HashSet<String>();

	public Set<String> getExcludedAttributeNames() {
		return excludedAttributeNames;
	}

	public void setExcludedAttributeNames(Set<String> excludedAttributeNames) {
		this.excludedAttributeNames = excludedAttributeNames;
	}
	
	
	

}
