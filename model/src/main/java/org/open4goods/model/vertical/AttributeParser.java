package org.open4goods.model.vertical;

import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.exceptions.ParseException;

public abstract class AttributeParser {

	public abstract String parse(ProductAttribute attr, AttributeConfig attributeConfig, VerticalConfig verticalConfig) throws ParseException;
	
	public String parse(String value, AttributeConfig attributeConfig, VerticalConfig verticalConfig) throws ParseException{
		return value;
	}

}
