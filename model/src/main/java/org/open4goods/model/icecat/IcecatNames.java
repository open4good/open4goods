package org.open4goods.model.icecat;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class IcecatNames {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Name")
    private List<IcecatName> names;

    // Getters and Setters
    public List<IcecatName> getNames() {
        return names;
    }

    public void setNames(List<IcecatName> names) {
        this.names = names;
    }
}