package org.open4goods.model.dto;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.model.data.FeatureGroup;
import org.open4goods.model.product.AggregatedAttribute;

/**
 * A convenient pojo holding feature groups and attributes,
 * for easy rendering on the templates sides
 */
public class UiFeatureGroups {

	private FeatureGroup featureGroup;
	
	// Shortcut to the localized name
	private String name;
	
	private List<AggregatedAttribute> attributes = new ArrayList<>();

	public FeatureGroup getFeatureGroup() {
		return featureGroup;
	}

	public void setFeatureGroup(FeatureGroup featureGroup) {
		this.featureGroup = featureGroup;
	}

	public List<AggregatedAttribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<AggregatedAttribute> attributes) {
		this.attributes = attributes;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}	
	
	
}
