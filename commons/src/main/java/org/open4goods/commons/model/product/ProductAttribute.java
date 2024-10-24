package org.open4goods.commons.model.product;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.model.data.UnindexedKeyValTimestamp;

public class ProductAttribute extends SourcableAttribute implements IAttribute {

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
	// TODO(p1, design) : remove after migration
	private Double numericValue;

	/**
	 * The collections of conflicts for this attribute
	 * 	 */
	// TODO(p1, design) : remove after migration
	private Set<UnindexedKeyValTimestamp> sources = new HashSet<>();


	
	/**
	 * The attribute raw rawValue
	 */
	private Set<Integer> icecatTaxonomyIds = new HashSet<>();
	
	
	

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof ProductAttribute) {
			return name.equals(((ProductAttribute) obj).name);
		}
		return false;
	}

	/**
	 * Add a "matched" attribute, with dynamic type detection
	 * 
	 * @param parsed Should handle language ?
	 */
	public void addSourceAttribute(SourcedAttribute attr)
			throws NumberFormatException {

		// Guard
		if (this.name != null && !name.equals(this.name)) {
			// TODO
			System.out.println("ERROR : Name mismatch in add attribute");
		}

		source.add(attr);

		value = bestValue();


	}

	

	@Override
	public String toString() {
		return name +":"+ value+   " ("+ source.size() + " source(s)";
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

//	public Set<UnindexedKeyValTimestamp> getSources() {
//		return sources;
//	}
//
//	public void setSources(Set<UnindexedKeyValTimestamp> sources) {
//		this.sources = sources;
//	}

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

	public Set<UnindexedKeyValTimestamp> getSources() {
		return sources;
	}

	public void setSources(Set<UnindexedKeyValTimestamp> sources) {
		this.sources = sources;
	}

	public Set<SourcedAttribute> getSource() {
		return source;
	}

	public void setSource(Set<SourcedAttribute> source) {
		this.source = source;
	}
	
	

}
