package org.open4goods.icecat.model;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.vertical.FeatureGroup;

/**
 * A convenient pojo holding feature groups and attributes,
 * for easy rendering on the templates sides
 */
public class AttributesFeatureGroups {

	private FeatureGroup featureGroup;
	
	// Shortcut to the localized name
	private String name;
	
	private List<ProductAttribute> attributes = new ArrayList<>();

	
	
	public List<ProductAttribute> features () {
		return attributes.stream().filter(e->e.isFeature()).toList();		
	}
	
	public List<ProductAttribute> unFeatures () {
		return attributes.stream().filter(e->e.isUnfeature()).toList();		
	}

	public List<ProductAttribute> attributes()  {
		return attributes.stream().filter(e-> (!e.isUnfeature() && ! e.isFeature())).toList();		
	}
	
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
