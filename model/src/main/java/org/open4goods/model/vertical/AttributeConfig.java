
package org.open4goods.model.vertical;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.Localisable;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * @author goulven
 *
 */
public class AttributeConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(AttributeConfig.class);

	private static final Map<String,AttributeParser> parserInstances = new HashMap<>();


	/**
	 * The identifier for this attribute.
	 */
	private String key;

	
	
	/**
	 * The associated font awesome icon
	 */
	private String faIcon = "fa-wrench";

	
	/**
	 * The localised units
	 */
	private Localisable<String,String> unit;
	
	
	/**
	 * The localised names
	 */
	private Localisable<String,String> name ;

	/**
	 * Indicates if it is numeric or terms filter. Will impact how the queries are made, and how the attribute is rendered in vertical search
	 */
	private AttributeType filteringType = AttributeType.TEXT;

	/**
	 * The icecat features id's this attribute is mapped to
	 */
	private Set<String> icecatFeaturesIds = new HashSet<String>(); 
	
	

	/**
	 * If true, this attribute will be added as a score, mapped through the numericMapping configuration attribute and an application of the scoring (min/max) mechanism
	 */
	private boolean asScore = false;

	/**
	 * If true, attribute value to score will be reversed (ie : for weight, the better score is the lowest weight)
	 */
	private boolean reverseScore = false;

	/**
	 * The ordering that must be applied to this attributes values after aggregations. (ie rendered in search attributes selection)
	 */
	private Order attributeValuesOrdering = Order.COUNT;

	/**
	 * If true, the ordering applied to aggregations will be reversed
	 */
	private Boolean attributeValuesReverseOrder = false;

	/**
	 * The attribute name that matches this attribute definition. key is datasourcename or "all" if applies on any.
	 */
	private Map<String, Set<String>> synonyms = new HashMap<>();


	/**
	 * The parser class for custom types / datas parsing
	 */
	private AttributeParserConfig parser = new AttributeParserConfig();

	/**
	 * If set, text attributes will be converted to numerics using this table conversion
	 */
	private Map<String,Double> numericMapping = new HashMap<>();


	/**
	 * If set, fixed text mappings conversion
	 */
	private Map<String,String> mappings = new HashMap<>();



	/**
	 *
	 * @return the instance of the defined parser, if set.
	 * @throws ResourceNotFoundException
	 */
	@JsonIgnore
	public AttributeParser getParserInstance() throws ResourceNotFoundException{


		final String clazz = parser.getClazz();
		if (StringUtils.isEmpty(clazz)) {
			throw new ResourceNotFoundException("No parser class defined for " + getKey());
		}

		AttributeParser parser = null;
		// Instanciating / caching the parser
		if (null == parserInstances.get(clazz)) {
			try {
				parser = (AttributeParser) Class.forName(clazz).getDeclaredConstructor().newInstance();
				LOGGER.info("{} parser has been placed in uidMap");
				parserInstances.put(clazz, parser);
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
				throw new ResourceNotFoundException("Cannot instanciate type : " + clazz,e);
			}
        }

		return parserInstances.get(clazz);
	}

	/**
	 *
	 * @param string
	 * @return
	 */
	public String applyCase(final String string) {
		if (parser.getLowerCase()) {
			return string.toLowerCase();
		}

		if (parser.getUpperCase()) {
			return string.toUpperCase();
		}
		return string;
	}

	/**
	 * Add a synomym to this attribute config
	 * @param store
	 * @param synonym
	 */
	public void addSynonym(final String store, final String synonym) {
		if (!synonyms.containsKey(store)) {
			synonyms.put(store,new HashSet<>());
		}

		synonyms.get(store).add(synonym);



	}


	/**
	 * If attributeconfig involves a rating translation, allows to get the max
	 * @return
	 * @throws ValidationException
	 * @throws NoSuchFieldException
	 */
	public Double maxRating() throws ValidationException, NoSuchFieldException {
		if (!asScore) {
			throw new ValidationException("Attribute is not configured to be translated as a rating");
		}

		if ( 0 == numericMapping.size()) {
			throw new ValidationException("Attribute is not configured to be translated as a rating, but has no numericMapping configuration");
		}

		return numericMapping.values().stream().max(Comparator.comparing(Double::valueOf)).orElseThrow();
	}


	/**
	 * If attributeconfig involves a rating translation, allows to get the min
	 * @return
	 * @throws ValidationException
	 * @throws NoSuchFieldException
	 */
	public Double minRating() throws ValidationException, NoSuchFieldException {
		if (!asScore) {
			throw new ValidationException("Attribute is not configured to be translated as a rating");
		}

		if ( 0 == numericMapping.size()) {
			throw new ValidationException("Attribute is not configured to be translated as a rating, but has no numericMapping configuration");
		}

		return numericMapping.values().stream().min(Comparator.comparing(Double::valueOf)).orElseThrow();
	}



	@Override
	public String toString() {
		return key + ":" + filteringType;
	}


	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof AttributeConfig) {
			String tmp = ((AttributeConfig)obj).getKey();
			if (null == tmp) {
				return tmp == key;
			}

			return key.equals(tmp);
		}

		return false;
	}




	public String i18n( final String language) {
		return name == null ? "null:"+language : name.i18n(language);
	}

	public AttributeType getFilteringType() {
		return filteringType;
	}


	public void setFilteringType(final AttributeType type) {
		this.filteringType = type;
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}



	public Map<String, Set<String>> getSynonyms() {
		return synonyms;
	}




	public void setSynonyms(final Map<String, Set<String>> synonyms) {
		this.synonyms = synonyms;
	}



	public String getFaIcon() {
		return faIcon;
	}

	public void setFaIcon(final String faIcon) {
		this.faIcon = faIcon;
	}


	public AttributeParserConfig getParser() {
		return parser;
	}

	public void setParser(final AttributeParserConfig parser) {
		this.parser = parser;
	}

	public Order getAttributeValuesOrdering() {
		return attributeValuesOrdering;
	}

	public void setAttributeValuesOrdering(final Order order) {
		attributeValuesOrdering = order;
	}

	public Boolean getAttributeValuesReverseOrder() {
		return attributeValuesReverseOrder;
	}

	public void setAttributeValuesReverseOrder(final Boolean reverseOrder) {
		attributeValuesReverseOrder = reverseOrder;
	}



	public Map<String, Double> getNumericMapping() {
		return numericMapping;
	}



	public void setNumericMapping(final Map<String, Double> numericMapping) {
		this.numericMapping = numericMapping;
	}

	public boolean isAsScore() {
		return asScore;
	}

	public void setAsScore(boolean asRating) {
		this.asScore = asRating;
	}

	public Localisable<String, String> getName() {
		return name;
	}

	public void setName(Localisable<String, String> name) {
		this.name = name;
	}

	public Map<String, String> getMappings() {
		return mappings;
	}

	public void setMappings(Map<String, String> mappings) {
		this.mappings = mappings;
	}


	public Set<String> getIcecatFeaturesIds() {
		return icecatFeaturesIds;
	}

	public void setIcecatFeaturesIds(Set<String> icecatFeaturesIds) {
		this.icecatFeaturesIds = icecatFeaturesIds;
	}

	public boolean isReverseScore() {
		return reverseScore;
	}

	public void setReverseScore(boolean reverseScore) {
		this.reverseScore = reverseScore;
	}

	public Localisable<String, String> getUnit() {
		return unit;
	}

	public void setUnit(Localisable<String, String> unit) {
		this.unit = unit;
	}


}
