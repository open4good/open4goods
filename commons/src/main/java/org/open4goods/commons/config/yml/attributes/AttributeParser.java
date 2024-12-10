package org.open4goods.commons.config.yml.attributes;

import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.exceptions.ParseException;
import org.open4goods.commons.model.attribute.Attribute;
import org.open4goods.commons.model.product.ProductAttribute;

public abstract class AttributeParser {

	public abstract String parse(ProductAttribute attr, AttributeConfig attributeConfig, VerticalConfig verticalConfig) throws ParseException;

}
