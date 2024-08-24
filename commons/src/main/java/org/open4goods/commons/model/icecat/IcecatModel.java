package org.open4goods.commons.model.icecat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatModel {
	@JacksonXmlProperty(localName = "Response")
	private IcecatResponse response;

	public IcecatResponse getResponse() {
		return response;
	}

	public void setResponse(IcecatResponse response) {
		this.response = response;
	}
}











