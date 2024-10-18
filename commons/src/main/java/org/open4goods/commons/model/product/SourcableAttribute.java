package org.open4goods.commons.model.product;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public abstract class SourcableAttribute {

	protected Set<SourcedAttribute> source = new HashSet<>();

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
	

	/**
	 * 
	 * @return the best value
	 */
	public String bestValue() {

		// Count values by unique keys... NOTE : Should have a java8+ nice solution here
		// !
		Map<String, Integer> valueCounter = new HashMap<>();

		for (SourcedAttribute source : source) {

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
		return source.stream().map(e -> e.getValue()).distinct().count();
	}

	
	/**
	 * Number of sources for this attribute
	 * 
	 * @return
	 */
	public int sourcesCount() {
		return source.size();
	}

	/**
	 * The number of different values for this item
	 * 
	 * @return
	 */
	public long distinctValues() {
		return source.stream().map(e->e.getValue()).distinct().count();
	}

	/**
	 * For UI, a String representation of all providers names
	 * 
	 * @return
	 */
	public String providersToString() {
		return StringUtils.join(source.stream().map(e->e.getDataSourcename()).toArray(), ", ");
	}

	/**
	 * For UI, a String representation of all providers names and values
	 * 
	 * @return
	 */
	public String sourcesToString() {
		return StringUtils.join(source.stream().map(e -> e.getDataSourcename() + ":" + e.getValue()).toArray(), ", ");

	}

	public Set<SourcedAttribute> getSource() {
		return source;
	}

	public void setSource(Set<SourcedAttribute> source) {
		this.source = source;
	}
	
	

}
