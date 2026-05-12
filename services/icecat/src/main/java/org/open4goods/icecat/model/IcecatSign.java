package org.open4goods.icecat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlText;

/** A localized unit-of-measure sign (e.g. "kg", "Go") from the Icecat catalog. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatSign {

    @JacksonXmlProperty(isAttribute = true, localName = "ID")
    private Integer id;

    @JacksonXmlProperty(isAttribute = true, localName = "langid")
    private int langId;

    @JacksonXmlProperty(isAttribute = true, localName = "Value")
    private String value;

    @JacksonXmlProperty(isAttribute = true, localName = "Updated")
    private String updated;

    /** Sign text carried as element text content (alternative to Value attribute). */
    @JacksonXmlText
    private String textValue;

    /** Returns the effective sign symbol regardless of which Icecat XML representation is used. */
    public String getEffectiveSign() {
        return value != null ? value : textValue;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public int getLangId() { return langId; }
    public void setLangId(int langId) { this.langId = langId; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getUpdated() { return updated; }
    public void setUpdated(String updated) { this.updated = updated; }

    public String getTextValue() { return textValue; }
    public void setTextValue(String textValue) { this.textValue = textValue; }
}
