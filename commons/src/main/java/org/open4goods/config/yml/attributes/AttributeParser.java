package org.open4goods.config.yml.attributes;

import org.open4goods.exceptions.ParseException;
import org.open4goods.model.attribute.Attribute;

public abstract class AttributeParser {

	public abstract String parse(String attrVal, Attribute attribute, AttributeConfig attributeConfig) throws ParseException;

}
