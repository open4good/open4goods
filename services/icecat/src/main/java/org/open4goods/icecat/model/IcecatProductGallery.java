package org.open4goods.icecat.model;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class IcecatProductGallery {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "ProductPicture")
    private List<IcecatProductPicture> productPicture;

    public List<IcecatProductPicture> getProductPicture() {
        return productPicture;
    }

    public void setProductPicture(List<IcecatProductPicture> productPicture) {
        this.productPicture = productPicture;
    }

}
