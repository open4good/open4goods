package org.open4goods.icecat.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class IcecatSummaryDescription {
    @JacksonXmlProperty(localName = "ShortSummaryDescription")
    private String shortSummaryDescription;

    @JacksonXmlProperty(localName = "LongSummaryDescription")
    private String longSummaryDescription;

    public String getShortSummaryDescription() {
        return shortSummaryDescription;
    }

    public void setShortSummaryDescription(String shortSummaryDescription) {
        this.shortSummaryDescription = shortSummaryDescription;
    }

    public String getLongSummaryDescription() {
        return longSummaryDescription;
    }

    public void setLongSummaryDescription(String longSummaryDescription) {
        this.longSummaryDescription = longSummaryDescription;
    }

}
