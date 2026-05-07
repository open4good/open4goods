package org.open4goods.icecat.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** Wrapper for a list of localized supplier names (the {@code <Names>} element inside a Supplier). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatSupplierNames {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Name")
    private List<IcecatSupplierName> names;

    public List<IcecatSupplierName> getNames() {
        return names != null ? names : Collections.emptyList();
    }

    public void setNames(List<IcecatSupplierName> names) {
        this.names = names;
    }
}
