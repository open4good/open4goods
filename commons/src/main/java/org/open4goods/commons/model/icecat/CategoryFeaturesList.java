package org.open4goods.commons.model.icecat;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public  class CategoryFeaturesList {
        @JacksonXmlProperty(isAttribute = true)
        private int Code;

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "Category")
        private List<IcecatCategory> categories;

		public int getCode() {
			return Code;
		}

		public void setCode(int code) {
			Code = code;
		}

		public List<IcecatCategory> getCategories() {
			return categories;
		}

		public void setCategories(List<IcecatCategory> categories) {
			this.categories = categories;
		}

        // Getters and setters
    }