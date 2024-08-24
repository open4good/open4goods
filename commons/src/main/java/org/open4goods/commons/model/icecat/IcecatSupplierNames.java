package org.open4goods.commons.model.icecat;
import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class IcecatSupplierNames {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Name")
    private List<IcecatSupplierName> names;

    // Getters et setters

    public List<IcecatSupplierName> getNames() {
        return names;
    }

    public void setNames(List<IcecatSupplierName> names) {
        this.names = names;
    }
}
