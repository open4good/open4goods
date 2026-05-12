package org.open4goods.icecat.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** Wrapper for a list of {@link IcecatDescription} entries (the {@code <Descriptions>} element). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatDescriptions {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Description")
    private List<IcecatDescription> descriptions;

    public List<IcecatDescription> getDescriptions() {
        return descriptions != null ? descriptions : Collections.emptyList();
    }

    public void setDescriptions(List<IcecatDescription> descriptions) {
        this.descriptions = descriptions;
    }
}
