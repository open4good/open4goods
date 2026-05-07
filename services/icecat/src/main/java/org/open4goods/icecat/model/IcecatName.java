package org.open4goods.icecat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

/**
 * Multilingual name entry from the Icecat catalog.
 *
 * <p>Icecat carries the display name in two different places depending on the XML file:
 * either as a {@code Value} XML attribute or as the element's text content.
 * Use {@link #getEffectiveName()} to resolve this transparently.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatName {

    @JacksonXmlProperty(isAttribute = true, localName = "ID")
    private Integer id;

    @JacksonXmlProperty(isAttribute = true, localName = "langid")
    private int langId;

    @JacksonXmlProperty(isAttribute = true, localName = "Updated")
    private String updated;

    /** Name carried as an XML attribute (used in features and feature-groups files). */
    @JacksonXmlProperty(isAttribute = true, localName = "Value")
    private String value;

    /** Name carried as element text content (used in categories and suppliers files). */
    @JacksonXmlText
    private String textValue;

    /**
     * Returns the effective display name regardless of which Icecat XML representation is used.
     * Prefers the {@code Value} attribute; falls back to element text content.
     */
    public String getEffectiveName() {
        return value != null ? value : textValue;
    }

    @Override
    public String toString() {
        return id + ", langid=" + langId + ":" + getEffectiveName();
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
