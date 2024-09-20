package org.open4goods.commons.model.product;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.model.attribute.Attribute;

public class AggregatedAttribute implements IAttribute {

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
	private Double numericValue;

	/**
	 * The whole values, associated with datasources
	 */
	private Map<String, Set<String>> sources = new HashMap<>();

	
	/**
	 * The icecat matched taxonomies, by it's name
	 */
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
		return sources.keySet().size();
	}

	/**
	 * For UI, a String representation of all providers names
	 * 
	 * @return
	 */
	public String providersToString() {
		   return StringUtils.join(
		            sources.values().stream().flatMap(Set::stream).collect(Collectors.toSet()),", ");
	}

	/**
	 * For UI, a String representation of all providers names and values
	 * 
	 * @return
	 */
	public String sourcesToString() {

        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, Set<String>> entry : sources.entrySet()) {
            String joinedValues = StringUtils.join(entry.getValue(), ", ");
            result.append(entry.getKey()).append(": ").append(joinedValues).append("\n");
        }
        
        return result.toString();
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

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof AggregatedAttribute) {
			return name.equals(((AggregatedAttribute) obj).name);
		}
		return false;
	}

	/**
	 * Add a "matched" attribute, with dynamic type detection
	 * 
	 * @param parsed Should handle language ?
	 */
	public void addAttribute(Attribute attr, String datasourceName, String value)
			throws NumberFormatException {

		// Guard
		if (this.name != null && !name.equals(this.name)) {
			System.out.println("ERROR : Name mismatch in add attribute");
		}

		this.name = attr.getName();
		
		if (!sources.containsKey(value)) {
			sources.put(value, new HashSet<String>());
		} 
		sources.get(value).add(datasourceName);

		value = bestValue();

		try {
			numericValue = numericOrNull(value);
		} catch (NumberFormatException e) {

		}
	}
	
	/**
	 * 
	 * @return the best value
	 */
	public String bestValue() {
		return sources.entrySet().stream().max(Comparator.comparingInt(entry -> entry.getValue().size())) 
				.map(Map.Entry::getKey) 
				.orElse(null); 
	}

	/**
	 * Return the number of distinct values
	 * 
	 * @return
	 */
	public long ponderedvalues() {
		return sources.size();
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


	@Override
	public String getLanguage() {
		// TODO : i18n
		return null;
	}

	public Map<String, Set<String>> getSources() {
		return sources;
	}

	public void setSources(Map<String, Set<String>> sources) {
		this.sources = sources;
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
