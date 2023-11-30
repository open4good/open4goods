
package org.open4goods.model.attribute;

import java.util.Objects;

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
	private String rawValue;

	public Attribute() {

	}

	public Attribute(final String name, final String value, final String language) {
		rawValue = value;
		this.name = name;
		this.language = language;
	}
	
	public Attribute(final String name, final String value) {
		rawValue = value;
		this.name = name;
	}

	@Override
	//	/**
	//	 * NOTE : A choice is made to identify Attributes ONLY by their names.
	//	 */
	public boolean equals(final Object obj) {

		if (obj instanceof Attribute o) {
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

//	/**
//	 * Type the rawValue to the given type
//	 * @throws ValidationException
//	 */
//	public void typeAttribute(final AttributeType type) throws ValidationException {
//		switch (type) {
//		case BOOLEAN:
//			AttributeType specialized = specializeBoolean();
//			if (specialized != AttributeType.BOOLEAN) {
//				throw new ValidationException("Attribute "+getName()+" cannot be casted to "+type+ " with value " + getRawValue().toString() );
//			}
//			break;
//		case NUMERIC:
//			specialized = specializeNumeric();
//			if (specialized != AttributeType.NUMERIC) {
//				throw new ValidationException("Attribute "+getName()+" cannot be casted to "+type+ " with value " + getRawValue().toString() );
//			}
//			break;
//		case TEXT:
//			// Nothing to do, text is default
//			break;
//
//		default:
//			throw new ValidationException("UNKNOWN TYPE :  Attribute "+getName()+" cannot be casted to unknown type "+type+ " with value " + getRawValue().toString() );
//		}
//	}


	public Double numericOrNull() {
		// Trying to specialize as numeric
		final String num = rawValue.toString().trim().replace(",", ".");

		try {
			return  Double.valueOf(num);

		} catch (final NumberFormatException e) {
			return null;
		}
	}

//	
//
//	/**
//	 * Specialize the attribute into a Boolean if possible
//	 *
//	 * @return
//	 * @throws ValidationException
//	 */
//	public AttributeType specializeNumeric() throws ValidationException {
//
//		final AttributeType type = getType();
//
//		if (type == AttributeType.NUMERIC) {
//			return  AttributeType.NUMERIC;
//		}
//
//		if (type != AttributeType.TEXT) {
//			throw new ValidationException("Cannot specialize non text attributes");
//		}
//
//		// Trying to specialize as numeric
//		final String num = rawValue.toString().replace(",", ".");
//
//		try {
//			final Double dblVal = Double.valueOf(num);
//			rawValue = dblVal;
//			return AttributeType.NUMERIC;
//		} catch (final NumberFormatException e) {
//			throw new ValidationException("Attribute "+getName()+" cannot be casted to numeric with value " + getRawValue().toString() );
//		}
//
//
//	}
//
//	/**
//	 * Specialize the attribute into a Boolean if possible
//	 *
//	 * @return
//	 * @throws ValidationException
//	 */
//	public AttributeType specializeBoolean() throws ValidationException {
//
//
//		final String val = rawValue.toString().toLowerCase().trim();
//
//		switch (val) {
//		case "true":
//		case "yes":
//		case "oui":
//		case "1":
//			rawValue = Boolean.TRUE;
//			return AttributeType.BOOLEAN;
//
//		case "no":
//		case "non":
//		case "false":
//		case "0":
//			rawValue = Boolean.FALSE;
//			return AttributeType.BOOLEAN;
//
//		default:
//			throw new ValidationException("Attribute "+getName()+" cannot be casted to boolean with value " + getRawValue().toString() );
//		}
//	}
//
//
//
//	/**
//	 * Return AttributeType corresponding to a given object type
//	 *
//	 * @param o
//	 * @return
//	 */
//	public  AttributeType getType() {
//		if (rawValue instanceof String) {
//			return AttributeType.TEXT;
//		} else if (rawValue instanceof Integer) {
//			return AttributeType.NUMERIC;
//		} else if (rawValue instanceof Long) {
//			return AttributeType.NUMERIC;
//		} else if (rawValue instanceof Double) {
//			return AttributeType.NUMERIC;
//		} else if (rawValue instanceof Boolean) {
//			return AttributeType.BOOLEAN;
//		}
//
//		else {
//			logger.warn("Unknown attribute type : " + rawValue.getClass().getSimpleName());
//			return null;
//		}
//	}
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

	public String getRawValue() {
		return rawValue;
	}

	public void setRawValue(final String rawValue) {
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