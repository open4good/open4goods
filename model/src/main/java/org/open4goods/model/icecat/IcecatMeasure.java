package org.open4goods.model.icecat;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class IcecatMeasure {
	@JacksonXmlProperty(isAttribute = true)
	private String ID;

	@JacksonXmlProperty(isAttribute = true)
	private String Sign;

	@JacksonXmlProperty(isAttribute = true)
	private String Updated;

	@JacksonXmlProperty(localName = "Signs")
	private IcecatSigns signs;

	public String getID() {
		return ID;
	}

	public void setID(String ID) {
		this.ID = ID;
	}

	public String getSign() {
		return Sign;
	}

	public void setSign(String Sign) {
		this.Sign = Sign;
	}

	public String getUpdated() {
		return Updated;
	}

	public void setUpdated(String Updated) {
		this.Updated = Updated;
	}

	public IcecatSigns getSigns() {
		return signs;
	}

	public void setSigns(IcecatSigns signs) {
		this.signs = signs;
	}
}