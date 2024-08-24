package org.open4goods.commons.model.icecat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatName {

    @JacksonXmlProperty(isAttribute = true, localName = "ID")
    private int id;

    @JacksonXmlProperty(isAttribute = true, localName = "langid")
    private int langId;

    @JacksonXmlProperty(isAttribute = true, localName = "Updated")
    private String updated;

    @JacksonXmlProperty(isAttribute = true, localName = "Value")
    private String value;

    @JacksonXmlText
    private String textValue;

    
    
	@Override
	public String toString() {
		return  id + ", langid=" + langId + ":" + value;
	}
	
    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLangId() {
        return langId;
    }

    public void setLangId(int langId) {
        this.langId = langId;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    // TODO : Micmac, should merge with getValue (either icecat bug, either using same POJO for 2 xsd definitions
	public String getTextValue() {
		return textValue;
	}

	public void setTextValue(String textValue) {
		this.textValue = textValue;
	}
    
    
}