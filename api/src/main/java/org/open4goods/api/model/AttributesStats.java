package org.open4goods.api.model;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.open4goods.model.attribute.ProductAttribute;

public class AttributesStats {

	/**
	 * Number of hits for this attribute
	 */
	private Integer hits = 0;
	private Set<String> datasourceNames = new HashSet<>();
	
	/**
	 * The values, with associated popularity
	 */
	private Map<String,Integer> values = new LinkedHashMap<>();

	public Map<String, Integer> getValues() {
		return values;
	}

	public void setValues(Map<String, Integer> values) {
		this.values = values;
	}
	
	/**
	 * Increments the stats
	 * @param key the key for the attribute
	 * @param value the ProductAttribute object
	 */
	public void process(String key, ProductAttribute value) {
	    hits++;

	    datasourceNames.addAll(value.getSource().stream().map(e->e.getDataSourcename()).toList() );
	    
	    // Incrementing stats by value (value.getValue())
	    // Assuming value.getValue() returns a string that represents the attribute value.
	    if (value != null && value.getValue() != null) {
	        values.compute(value.getValue(), (k, v) -> v == null ? 1 : v + 1);
	    }
	}

	public Integer getHits() {
		return hits;
	}

	public void setHits(Integer hits) {
		this.hits = hits;
	}

	/**
	 * Sorts the values map by integer value in descending order.
	 */
	public void sort() {
	    values = values.entrySet()
	            .stream()
	            .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue())) // Descending order
	            .collect(LinkedHashMap::new, // Collect into a LinkedHashMap to maintain order
	                    (map, entry) -> map.put(entry.getKey(), entry.getValue()),
	                    LinkedHashMap::putAll);
	}

	public Set<String> getDatasourceNames() {
		return datasourceNames;
	}

	public void setDatasourceNames(Set<String> datasourceNames) {
		this.datasourceNames = datasourceNames;
	}

	
	
	
}
