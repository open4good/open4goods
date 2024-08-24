package org.open4goods.commons.model.icecat;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public class IcecatSign {
	@JacksonXmlProperty(isAttribute = true)
	private String ID;

	@JacksonXmlProperty(isAttribute = true)
	private String langid;

	@JacksonXmlProperty(isAttribute = true)
	private String Updated;

	@JacksonXmlText
	private String value;

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

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}