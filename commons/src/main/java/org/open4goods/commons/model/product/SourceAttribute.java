package org.open4goods.commons.model.product;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.config.yml.attributes.AttributeConfig;
import org.open4goods.commons.model.attribute.Attribute;
import org.open4goods.commons.model.attribute.AttributeType;
import org.open4goods.commons.model.data.UnindexedKeyVal;
import org.open4goods.commons.model.data.UnindexedKeyValTimestamp;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class SourceAttribute implements IAttribute {

	/**
	 * The name of this aggregated attribute
	 */
	private String name;

	/**
	 * The value of this aggregated attribute
	 */
	private String value;

	/**
	 * The numeric value (if any) of this aggregated attribute
	 */
	@Field(index = true, store = false, type = FieldType.Double)
	private Double numericValue;

	/**
	 * The collections of conflicts for this attribute
	 */
	@Field( index = false, store = false, enabled = false, type = FieldType.Object)
	private Set<UnindexedKeyValTimestamp> sources = new HashSet<>();

	
	/**
	 * The attribute raw rawValue
	 */
	@Field(index = false, store = false, type = FieldType.Keyword)
	private Set<Integer> icecatTaxonomyIds = new HashSet<>();
	
	
	/**
	 * Number of sources for this attribute
	 * 
	 * @return
	 */
	public int sourcesCount() {
		return sources.size();
	}

	/**
	 * The number of different values for this item
	 * 
	 * @return
	 */
	public long distinctValues() {
		return sources.stream().map(UnindexedKeyVal::getValue).distinct().count();
	}

	/**
	 * For UI, a String representation of all providers names
	 * 
	 * @return
	 */
	public String providersToString() {
		return StringUtils.join(sources.stream().map(UnindexedKeyVal::getKey).toArray(), ", ");
	}

	/**
	 * For UI, a String representation of all providers names and values
	 * 
	 * @return
	 */
	public String sourcesToString() {
		return StringUtils.join(sources.stream().map(e -> e.getKey() + ":" + e.getValue()).toArray(), ", ");

	}

	public boolean hasConflicts() {
		return distinctValues() > 1;
	}

	public String bgRow() {
		String ret = "table-default";
		int sCount = sourcesCount();
		long dValues = distinctValues();

		if (sCount == 0) {
			ret = "table-danger";
		} else if (sCount == 1) {
			ret = "table-default";
		} else {
			ret = "table-info";
		}

		if (dValues > 1) {
			ret = "table-danger";
		}

		return ret;
	}

	// TODO : Simple, but does not allow to handle conflicts, and so on
	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof SourceAttribute) {
			return name.equals(((SourceAttribute) obj).name);
		}
		return false;
	}

	/**
	 * Add a "matched" attribute, with dynamic type detection
	 * 
	 * @param parsed Should handle language ?
	 */
	public void addSourceAttribute(Attribute attr, AttributeConfig attrConfig, UnindexedKeyValTimestamp sourcedValue)
			throws NumberFormatException {

		// Guard
		if (this.name != null && !name.equals(this.name)) {
			// TODO
			System.out.println("ERROR : Name mismatch in add attribute");
		}

		this.name = attr.getName();
		sources.add(sourcedValue);

		value = bestValue();

		if (attrConfig.getType().equals(AttributeType.NUMERIC)) {
			numericValue = numericOrNull(value);
		}
	}

	public void addAttribute(Attribute attr, UnindexedKeyValTimestamp sourcedValue) {
		// Guard
		if (this.name != null && !name.equals(this.name)) {
			// TODO
			System.out.println("ERROR : Name mismatch in add attribute");
		}

		this.name = attr.getName();
		sources.add(sourcedValue);

		value = bestValue();

	}

	/**
	 * 
	 * @return the best value
	 */
	public String bestValue() {

		// Count values by unique keys... NOTE : Should have a java8+ nice solution here
		// !
		Map<String, Integer> valueCounter = new HashMap<>();

		for (UnindexedKeyValTimestamp source : sources) {

			valueCounter.merge(source.getValue(), 1, Integer::sum);
		}

		// sort this map by values

		Map<String, Integer> result = valueCounter.entrySet().stream()
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))

				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue,
						LinkedHashMap::new));

		// Take the first one : will be the "most recommanded" if discriminant number of
		// datasources, a random value otherwise

		return result.entrySet().stream().findFirst().get().getKey();
	}

	/**
	 * Return the number of distinct values
	 * 
	 * @return
	 */
	public long ponderedvalues() {
		return sources.stream().map(UnindexedKeyVal::getValue).distinct().count();
	}

	@Override
	public String toString() {
		return name + " : " + value + " -> " + sources.size() + " source(s), " + ponderedvalues() + " conflict(s)";
	}

	public Double numericOrNull(String rawValue) throws NumberFormatException {
		// Trying to specialize as numeric
		final String num = rawValue.trim().replace(",", ".");

		return Double.valueOf(num);
	}

	///////////////////////////////////////
	// Getters / Setters
	///////////////////////////////////////

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

//	public AttributeType getType() {
//		return type;
//	}
//
//
//	public void setType(AttributeType type) {
//		this.type = type;
//	}

	public Set<UnindexedKeyValTimestamp> getSources() {
		return sources;
	}

	public void setSources(Set<UnindexedKeyValTimestamp> sources) {
		this.sources = sources;
	}

	@Override
	public String getLanguage() {
		// TODO : i18n
		return null;
	}

	public Double getNumericValue() {
		return numericValue;
	}

	public void setNumericValue(Double numericValue) {
		this.numericValue = numericValue;
	}

	public Set<Integer> getIcecatTaxonomyIds() {
		return icecatTaxonomyIds;
	}

	public void setIcecatTaxonomyIds(Set<Integer> icecatTaxonomyIds) {
		this.icecatTaxonomyIds = icecatTaxonomyIds;
	}
	
	

}