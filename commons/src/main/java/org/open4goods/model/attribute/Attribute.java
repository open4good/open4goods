
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
 *
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
		if (multivalued()) {
			return StringUtils.join(rawValue,", ");
		} else {
			return rawValue.toString();
		}
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

	////////////////////////////////////////////////
	// Utility
	////////////////////////////////////////////////

	/**
	 *
	 * @return number of values for this elem
	 */

	public int valuesSize() {
		if (multivalued()) {
			return ((Collection) rawValue).size();
		}
		return 1;
	}

	/**
	 * Return true if this is a multivalued attribute
	 * @return
	 */
	public boolean multivalued() {
		return rawValue instanceof Collection;
	}

	/**
	 * Normalize the value
	 */
	public void normalize() {
		if (multivalued()) {
			final List<String> ret = new ArrayList<>();
			for (final Object o : (Collection) rawValue) {
				ret.add(StringUtils.normalizeSpace(o.toString()));
			}
			rawValue = ret;

		} else {
			rawValue = StringUtils.normalizeSpace(rawValue.toString());
		}
	}

	/**
	 * Remove empty "lines" (if present) in multi valued attributes
	 */
	public void cleanMultiValues () {
		if (multivalued()) {
			final List<String> ret = new ArrayList<>();
			for (final Object o : (Collection) rawValue) {
				if (!StringUtils.isBlank(o.toString())) {
					ret.add(o.toString());
				}
			}
			rawValue = ret;
		}
	}

	/**
	 * Trim the value
	 */
	public void trim() {
		if (multivalued()) {
			final List<String> ret = new ArrayList<>();
			for (final Object o : (Collection) rawValue) {
				ret.add(o.toString().trim());
			}
			rawValue = ret;

		} else {
			rawValue = rawValue.toString().trim();
		}
	}

	/**
	 * Convert to lowerCase
	 */
	public void lowerCase() {

		if (multivalued()) {
			final List<String> ret = new ArrayList<>();
			for (final Object o : (Collection) rawValue) {
				ret.add(o.toString().toLowerCase());
			}
			rawValue = ret;
		} else {
			rawValue = rawValue.toString().toLowerCase();
		}
	}

	/**
	 * Convert to upperCase
	 */
	public void upperCase() {
		if (multivalued()) {
			final List<String> ret = new ArrayList<>();
			for (final Object o : (Collection) rawValue) {
				ret.add(o.toString().toUpperCase());
			}
			rawValue = ret;
		} else {
			rawValue = rawValue.toString().toUpperCase();
		}
	}

	/**
	 * Remove a text token from the attribute value(s)
	 *
	 */
	public void replaceToken(final String regexp, final String replacement) {

		// NOTE(gof) : The type() method requires perf, but is good on a safety perspective
		final Optional<AttributeType> type = detectType();
		if (type.isPresent()) {
			if (!type.get().equals(AttributeType.TEXT)) {
				logger.warn("Cannot replace token {} from non TEXT attributes ({} is {})", regexp, type.get());
			} else {

				if (multivalued()) {
					final Set<String> ret = new HashSet<>();
					for (final Object o : (Collection) rawValue) {
						ret.add(((String) o).replace(regexp, replacement));
					}
					rawValue = ret;
				} else {
					rawValue = ((String) rawValue).replace(regexp, replacement);
				}
			}
		}
	}

	/**
	 *
	 * @return The type of an attribute
	 */
	public Optional<AttributeType> detectType() {
		if (rawValue instanceof Collection) {
			// Getting the first elem type
			final Optional o = ((Collection) rawValue).stream().findAny();
			if (o.isPresent()) {
				return Optional.ofNullable(getType(o.get()));
			} else {
				logger.warn("Cannot detect type of empty Collections for attribute {}", this);
				return Optional.empty();
			}
		} else {
			return Optional.ofNullable(getType(rawValue));
		}
	}

	/**
	 * Type the rawValue to the given type
	 * @throws ValidationException
	 */
	public void typeAttribute(final AttributeType type) throws ValidationException {
		switch (type) {
		case BOOLEAN:
			AttributeType specialized = specializeBoolean();
			if (specialized != AttributeType.BOOLEAN) {
				throw new ValidationException("Attribute "+getName()+" cannot be casted to "+type+ " with value " + getRawValue().toString() );
			}
			break;
		case NUMERIC:
			specialized = specializeNumeric();
			if (specialized != AttributeType.NUMERIC) {
				throw new ValidationException("Attribute "+getName()+" cannot be casted to "+type+ " with value " + getRawValue().toString() );
			}
			break;
		case TEXT:
			// Nothing to do, text is default
			break;

		default:
			throw new ValidationException("UNKNOWN TYPE :  Attribute "+getName()+" cannot be casted to unknown type "+type+ " with value " + getRawValue().toString() );
		}
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

	/**
	 * Split a raw attribute string into a multivalued string attribute
	 *
	 * @param multivalueSeparator
	 */
	public void multivalue(final Set<String> multivalueSeparator) {

		/*
		 * Putting in efficient uidMap the split terms
		 */
		if (null != multivalueSeparator && multivalueSeparator.size() > 0) {
			for (final String spliters : multivalueSeparator ) {
				if (!splitsCache.containsKey(spliters)) {
					splitsCache.put(spliters, Pattern.compile(spliters));
				}
			}
		}

		if (getType(rawValue) == AttributeType.TEXT) {

			if (null != multivalueSeparator && multivalueSeparator.size() > 0) {
				final Set<String> vals = new HashSet<>();

				for (final String splitter : multivalueSeparator) {

					final List<String> tmpRet = Arrays.asList(splitsCache.get(splitter).split(rawValue.toString())).stream().map(String::trim).collect(Collectors.toList());
					if (tmpRet.size() > 1) {
						vals.addAll(tmpRet);
					}
				}

				if (vals.size() > 1) {
					rawValue = vals;
				}
			}
		}

		// Removing empty multilines
		cleanMultiValues();
	}

	/**
	 * Specialize the attribute into a Boolean if possible
	 *
	 * @return
	 * @throws ValidationException
	 */
	public AttributeType specializeNumeric() throws ValidationException {
		if (multivalued()) {
			throw new ValidationException("Cannot specialize multivalued attributes");
		}

		final AttributeType type = getType(rawValue);

		if (type == AttributeType.NUMERIC) {
			return  AttributeType.NUMERIC;
		}

		if (type != AttributeType.TEXT) {
			throw new ValidationException("Cannot specialize non text attributes");
		}

		// Trying to specialize as numeric
		final String num = rawValue.toString().replace(",", ".");

		try {
			final Double dblVal = Double.valueOf(num);
			rawValue = dblVal;
			return AttributeType.NUMERIC;
		} catch (final NumberFormatException e) {
			throw new ValidationException("Attribute "+getName()+" cannot be casted to numeric with value " + getRawValue().toString() );
		}


	}

	/**
	 * Specialize the attribute into a Boolean if possible
	 *
	 * @return
	 * @throws ValidationException
	 */
	public AttributeType specializeBoolean() throws ValidationException {
		if (multivalued()) {
			throw new ValidationException("Cannot specialize multivalued attributes");
		}

		final String val = rawValue.toString().toLowerCase().trim();

		switch (val) {
		case "true":
		case "yes":
		case "oui":
		case "1":
			rawValue = Boolean.TRUE;
			return AttributeType.BOOLEAN;

		case "no":
		case "non":
		case "false":
		case "0":
			rawValue = Boolean.FALSE;
			return AttributeType.BOOLEAN;

		default:
			throw new ValidationException("Attribute "+getName()+" cannot be casted to boolean with value " + getRawValue().toString() );
		}
	}

	/**
	 *
	 * @return String representation of the attribute value(s)
	 */
	public Set<String> stringValues() {
		final Set<String> ret = new HashSet<>();
		if (multivalued()) {
			((Collection) rawValue).stream().forEach(e -> ret.add(e.toString()));
		} else {
			ret.add(rawValue.toString());
		}
		return ret;
	}

	/**
	 * Return AttributeType corresponding to a given object type
	 *
	 * @param o
	 * @return
	 */
	public static AttributeType getType(final Object o) {
		if (o instanceof String) {
			return AttributeType.TEXT;
		} else if (o instanceof Integer) {
			return AttributeType.NUMERIC;
		} else if (o instanceof Long) {
			return AttributeType.NUMERIC;
		} else if (o instanceof Double) {
			return AttributeType.NUMERIC;
		} else if (o instanceof Boolean) {
			return AttributeType.BOOLEAN;
		}

		else {
			logger.warn("Unknown attribute type : " + o.getClass().getSimpleName());
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