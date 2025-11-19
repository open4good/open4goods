package org.open4goods.icecat.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class IcecatMultimediaObject {
    @JacksonXmlProperty(isAttribute = true)
    private String URL;

    @JacksonXmlProperty(isAttribute = true)
    private String ThumbUrl;

    @JacksonXmlProperty(isAttribute = true)
    private String ContentType;

    @JacksonXmlProperty(isAttribute = true)
    private String Description;

    @JacksonXmlProperty(isAttribute = true)
    private String Type;

    @JacksonXmlProperty(isAttribute = true)
    private String ID;

    public String getURL() {
        return URL;
    }

    public void setURL(String uRL) {
        URL = uRL;
    }

    public String getThumbUrl() {
        return ThumbUrl;
    }

    public void setThumbUrl(String thumbUrl) {
        ThumbUrl = thumbUrl;
    }

    public String getContentType() {
        return ContentType;
    }

    public void setContentType(String contentType) {
        ContentType = contentType;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

    public String getID() {
        return ID;
    }

    public void setID(String iD) {
        ID = iD;
    }

}
