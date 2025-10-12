package org.open4goods.icecat.model;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class IcecatDescriptions {
	@JacksonXmlElementWrapper(useWrapping = false)
	@JacksonXmlProperty(localName = "Description")
	private List<IcecatDescription> descriptions;

	public List<IcecatDescription> getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(List<IcecatDescription> descriptions) {
		this.descriptions = descriptions;
	}
}