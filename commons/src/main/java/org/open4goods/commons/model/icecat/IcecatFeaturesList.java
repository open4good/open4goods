package org.open4goods.commons.model.icecat;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class IcecatFeaturesList {
	@JacksonXmlElementWrapper(useWrapping = false)
	@JacksonXmlProperty(localName = "Feature")
	private List<IcecatFeature> features;

	public List<IcecatFeature> getFeatures() {
		return features;
	}

	public void setFeatures(List<IcecatFeature> features) {
		this.features = features;
	}
}
