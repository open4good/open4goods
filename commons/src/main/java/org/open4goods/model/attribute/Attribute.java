
package org.open4goods.model.attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Validable;
import org.open4goods.model.product.IAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Attribute is semi-structured piece of informations fetched from DataFragments. They
 * are also handled as product informations by dedicated sites once standardized.
 *
 * @author goulven
 *TODO : performance, remove multivalued, specialization
 */
@SuppressWarnings("rawtypes")
public class Attribute implements Validable,IAttribute {

	private final static Logger logger = LoggerFactory.getLogger(Attribute.class);

	private static final Map<String, Pattern> splitsCache = new HashMap<>();

	/**
	 * The attribute name
	 */
	@Field(index = true, store = false, type = FieldType.Text)
	private String name;

	
	/**
	 * The attribute language, if pertinent
	 */
	@Field(index = true, store = false, type = FieldType.Keyword)
	private String language;

	/**
	 * The attribute raw rawValue
	 */
	@Field(index = false, store = false, type = FieldType.Keyword)
	//	TODO : Pass to String
	private Object rawValue;

	public Attribute() {

	}

	public Attribute(final String name, final Object value, final String language) {
		rawValue = value;
		this.name = name;
	}
	
	public Attribute(final String name, final Object value) {
		rawValue = value;
		this.name = name;
	}

	@Override
	//	/**
	//	 * NOTE : A choice is made to identify Attributes ONLY by their names.
	//	 */
	public boolean equals(final Object obj) {

		if (obj instanceof Attribute) {
			final Attribute o = (Attribute) obj;
			return Objects.equals(name, o.getName());
		}
		return false;
	}

	@Override
	public String toString() {
		return name + ":" + rawValue;
	}


	public String stringValue() {

			return rawValue.toString();
	}




	@Override
	public void validate() throws ValidationException {
		if (StringUtils.isEmpty(name)) {
			throw new ValidationException("Empty name");
		}

		if (null == rawValue) {
			throw new ValidationException("No value");
		}
	}



	/**
	 * Normalize the value
	 */
	public void normalize() {
		rawValue = StringUtils.normalizeSpace(rawValue.toString());
	}

	

	/**
	 * Trim the value
	 */
	public void trim() {

			rawValue = rawValue.toString().trim();
		
	}

	/**
	 * Convert to lowerCase
	 */
	public void lowerCase() {
			rawValue = rawValue.toString().toLowerCase();
	}

	/**
	 * Convert to upperCase
	 */
	public void upperCase() {
			rawValue = rawValue.toString().toUpperCase();
	}

	/**
	 * Remove a text token from the attribute value(s)
	 *
	 */
	public void replaceToken(final String regexp, final String replacement) {

		rawValue = ((String) rawValue).replace(regexp, replacement);
	}


	public Double numericOrNull() {
		// Trying to specialize as numeric
		final String num = rawValue.toString().replace(",", ".");

		try {
			return  Double.valueOf(num);

		} catch (final NumberFormatException e) {
			return null;
		}
	}

	
	////////////////////////////////////////////////
	// Getters and setters
	////////////////////////////////////////////////

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(final String name) {
		this.name = name;
	}

	public Object getRawValue() {
		return rawValue;
	}

	public void setRawValue(final Object rawValue) {
		this.rawValue = rawValue;
	}

	@Override
	public String getLanguage() {
		return language;
	}

	public void setLanguage(final String language) {
		this.language = language;
	}

	@Override
	public String getValue() {
		return rawValue.toString();
	}





}