package org.open4goods.icecat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlText;

/** A localized textual description entry from the Icecat catalog. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatDescription {

    @JacksonXmlProperty(isAttribute = true, localName = "ID")
    private Integer id;

    @JacksonXmlProperty(isAttribute = true, localName = "langid")
    private int langId;

    @JacksonXmlProperty(isAttribute = true, localName = "Updated")
    private String updated;

    @JacksonXmlProperty(isAttribute = true, localName = "Value")
    private String value;

    @JacksonXmlText
    private String textValue;

    /** Returns the effective description text regardless of which Icecat XML representation is used. */
    public String getEffectiveValue() {
        return value != null ? value : textValue;
    }

    @Override
    public String toString() {
        return id + ", langid=" + langId + ":" + getEffectiveValue();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public int getLangId() { return langId; }
    public void setLangId(int langId) { this.langId = langId; }

    public String getUpdated() { return updated; }
    public void setUpdated(String updated) { this.updated = updated; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getTextValue() { return textValue; }
    public void setTextValue(String textValue) { this.textValue = textValue; }
}
