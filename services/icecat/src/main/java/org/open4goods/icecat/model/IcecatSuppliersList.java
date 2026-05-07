package org.open4goods.icecat.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/** Wrapper for the {@code <SuppliersList>} response element. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "SuppliersList")
public class IcecatSuppliersList {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Supplier")
    private List<IcecatSupplier> suppliers;

    public List<IcecatSupplier> getSuppliers() {
        return suppliers != null ? suppliers : Collections.emptyList();
    }

    public void setSuppliers(List<IcecatSupplier> suppliers) {
        this.suppliers = suppliers;
    }
}
