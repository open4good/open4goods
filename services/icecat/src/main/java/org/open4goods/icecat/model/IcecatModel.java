package org.open4goods.icecat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatModel {
	@JacksonXmlProperty(localName = "Response")
	private IcecatResponse response;

	@JacksonXmlProperty(localName = "Product")
	private IcecatProduct product;

	public IcecatResponse getResponse() {
		return response;
	}

	public void setResponse(IcecatResponse response) {
		this.response = response;
	}

	public IcecatProduct getProduct() {
		return product;
	}

	public void setProduct(IcecatProduct product) {
		this.product = product;
	}
}
