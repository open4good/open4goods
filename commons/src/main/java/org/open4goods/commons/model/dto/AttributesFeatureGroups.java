package org.open4goods.commons.model.dto;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.commons.model.data.FeatureGroup;
import org.open4goods.commons.model.product.ProductAttribute;

/**
 * A convenient pojo holding feature groups and attributes,
 * for easy rendering on the templates sides
 */
public class AttributesFeatureGroups {

	private FeatureGroup featureGroup;
	
	// Shortcut to the localized name
	private String name;
	
	private List<ProductAttribute> attributes = new ArrayList<>();

	public FeatureGroup getFeatureGroup() {
		return featureGroup;
	}

	public void setFeatureGroup(FeatureGroup featureGroup) {
		this.featureGroup = featureGroup;
	}

	public List<ProductAttribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<ProductAttribute> attributes) {
		this.attributes = attributes;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}	
	
	
}
