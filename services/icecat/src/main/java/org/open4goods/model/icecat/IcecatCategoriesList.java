package org.open4goods.model.icecat;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class IcecatCategoriesList {

	  @JacksonXmlProperty(localName = "Category")
	    @JacksonXmlElementWrapper(useWrapping = false)
	    private List<IcecatCategory> categories;

	    // Getters et setters

	    public List<IcecatCategory> getCategories() {
	        return categories;
	    }

	    public void setCategories(List<IcecatCategory> categories) {
	        this.categories = categories;
	    }
}
