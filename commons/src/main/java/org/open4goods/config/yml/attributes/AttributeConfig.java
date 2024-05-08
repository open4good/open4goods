
package org.open4goods.config.yml.attributes;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Localisable;
import org.open4goods.model.attribute.AttributeType;
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

	private Localisable name ;

	private AttributeType type = AttributeType.TEXT;


	/**
	 * If true, this attribute will be added as a score, mapped through the numericMapping configuration attribute and an application of the scoring (min/max) mechanism
	 */
	// TODO : Rename to asScore
	private boolean asRating = false;

	/**
	 * If true, this attribute will be used as search filter
	 */
	private boolean asSearchFilter = true;

	
		
	

	/**
	 * The ordering that must be applied to this attributes values after aggregations. (ie rendered in search attributes selection)
	 */
	private Order attributeValuesOrdering = Order.ALPHA;

	/**
	 * If true, the ordering applied to aggregations will be reversed
	 */
	private Boolean attributeValuesReverseOrder = false;









	/** If true, this attribute will be used for aggregations searches **/
	// private Boolean searchable = false;

	/**
	 * The position in the search page navigation zone
	 */
	// private Integer searchPresentationOrder = Integer.MIN_VALUE;

	/**
	 * If set, this attribute will appear at the given attributeValuesOrdering in tabularised
	 * search results
	 **/
	private Integer searchTableOrder;

	/**
	 * If set, erase the default attributes typed template to use the defined
	 * one
	 **/
	private String searchTemplate;


//
//	/**
//	 * If non null, present the "count by" stats for this attribute, Will appear at the index designated by the integer value
//	 */
//	private Integer statsOrder;
//
//	/**
//	 * the Chart.js chart type
//	 */
//	private AttrChartType statsType = AttrChartType.bar;
//
//	/**
//	 * If true, will be the default stats rendered. (Should so only have one
//	 * attribute set to true)
//	 */
//	private boolean statsDefaultView = false;










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
		if (!asRating) {
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
		if (!asRating) {
			throw new ValidationException("Attribute is not configured to be translated as a rating");
		}

		if ( 0 == numericMapping.size()) {
			throw new ValidationException("Attribute is not configured to be translated as a rating, but has no numericMapping configuration");
		}

		return numericMapping.values().stream().min(Comparator.comparing(Double::valueOf)).orElseThrow();
	}



	@Override
	public String toString() {
		return key + ":" + type;
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

	public AttributeType getType() {
		return type;
	}


	public void setType(final AttributeType type) {
		this.type = type;
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



	/**
	 * If set, this attribute will appear at the given attributeValuesOrdering in tabularised
	 * search results
	 **/
	public Integer getSearchTableOrder() {
		return searchTableOrder;
	}

	public void setSearchTableOrder(final Integer searchTableOrder) {
		this.searchTableOrder = searchTableOrder;
	}

	public String getFaIcon() {
		return faIcon;
	}

	public void setFaIcon(final String faIcon) {
		this.faIcon = faIcon;
	}


	public String getSearchTemplate() {
		return searchTemplate;
	}

	public void setSearchTemplate(final String searchTemplate) {
		this.searchTemplate = searchTemplate;
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

	public boolean isAsRating() {
		return asRating;
	}

	public void setAsRating(boolean asRating) {
		this.asRating = asRating;
	}



	public Localisable getName() {
		return name;
	}

	public void setName(final Localisable name) {
		this.name = name;
	}

	public Map<String, String> getMappings() {
		return mappings;
	}

	public void setMappings(Map<String, String> mappings) {
		this.mappings = mappings;
	}

	public boolean isAsSearchFilter() {
		return asSearchFilter;
	}

	public void setAsSearchFilter(boolean asSearchFilter) {
		this.asSearchFilter = asSearchFilter;
	}


}
