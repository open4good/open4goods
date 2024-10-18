package org.open4goods.commons.config.yml.attributes;

import org.open4goods.commons.exceptions.ParseException;
import org.open4goods.commons.model.attribute.Attribute;

public abstract class AttributeParser {

	public abstract String parse(String attrVal, AttributeConfig attributeConfig) throws ParseException;

}
