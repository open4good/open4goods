package org.open4goods.icecat.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** Wrapper for the {@code <CategoriesList>} response element. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatCategoriesList {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Category")
    private List<IcecatCategory> categories;

    public List<IcecatCategory> getCategories() {
        return categories != null ? categories : Collections.emptyList();
    }

    public void setCategories(List<IcecatCategory> categories) {
        this.categories = categories;
    }
}
