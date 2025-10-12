package org.open4goods.icecat.model;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

class IcecatSigns {
	@JacksonXmlElementWrapper(useWrapping = false)
	@JacksonXmlProperty(localName = "Sign")
	private List<IcecatSign> signs;

	public List<IcecatSign> getSigns() {
		return signs;
	}

	public void setSigns(List<IcecatSign> signs) {
		this.signs = signs;
	}
}