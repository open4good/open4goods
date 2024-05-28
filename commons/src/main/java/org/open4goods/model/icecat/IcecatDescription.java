package org.open4goods.model.icecat;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

class IcecatDescription {
	@JacksonXmlProperty(isAttribute = true)
	private String ID;

	@JacksonXmlProperty(isAttribute = true)
	private String langid;

	@JacksonXmlProperty(isAttribute = true)
	private String Updated;

	@JacksonXmlText
	private String text;

	public String getID() {
		return ID;
	}

	public void setID(String ID) {
		this.ID = ID;
	}

	public String getLangid() {
		return langid;
	}

	public void setLangid(String langid) {
		this.langid = langid;
	}

	public String getUpdated() {
		return Updated;
	}

	public void setUpdated(String Updated) {
		this.Updated = Updated;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
