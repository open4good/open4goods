package org.open4goods.model.icecat;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "SuppliersList")
public class IcecatSuppliersList {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Supplier")
    private List<IcecatSupplier> suppliers;

    // Getters et setters

    public List<IcecatSupplier> getSuppliers() {
        return suppliers;
    }

    public void setSuppliers(List<IcecatSupplier> suppliers) {
        this.suppliers = suppliers;
    }
}