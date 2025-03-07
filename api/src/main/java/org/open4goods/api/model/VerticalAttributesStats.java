package org.open4goods.api.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.model.attribute.ProductAttribute;

/**
 * Repesents attributes stats for a vertical
 */
public class VerticalAttributesStats {

	private Integer totalItems = 0;

	/**
	 * Stats by attribute name
	 */
	private Map<String, AttributesStats> stats = new LinkedHashMap<>();

	public Integer getTotalItems() {
		return totalItems;
	}

	public void setTotalItems(Integer totalItems) {
		this.totalItems = totalItems;
	}

	public Map<String, AttributesStats> getStats() {
		return stats;
	}

	public void setStats(Map<String, AttributesStats> stats) {
		this.stats = stats;
	}

	/**
	 * Increments the stats with provided attributes
	 * @param all
	 */
	public void process(Map<String, ProductAttribute> attrs) {
		totalItems++;
		
		for (Entry<String, ProductAttribute> attrEntry : attrs.entrySet()) {
			AttributesStats as = stats.get(attrEntry.getKey());
			
			if (null == as) {
				as = new AttributesStats();
			}
			
			as.process(attrEntry.getKey(), attrEntry.getValue());
			stats.put(attrEntry.getKey(), as);
			
		}
	}

	/**
	 * Sort the data for better restitution
	 */
	public void sort() {
		
		// Sorting the attribute names 
	    stats = stats.entrySet()
	            .stream()
	            .sorted((e1, e2) -> Integer.compare(e2.getValue().getHits(), e1.getValue().getHits())) // Descending order
	            .collect(LinkedHashMap::new, // Collect into a LinkedHashMap to maintain order
	                    (map, entry) -> map.put(entry.getKey(), entry.getValue()),
	                    LinkedHashMap::putAll);
	    
	   // Sorting the attributes values frequency
	    stats.values().forEach(e -> {
	    	e.sort();
	    });
	    
	    
	}

	/**
	 * Cleaning by evicting dummy / noisy values
	 */
	public void clean() {
		// Removing items where hits = number of attributes (means 1 to 1, like descriptif, title,gtin...)
		Set<String> toRemove = stats.entrySet().stream().filter(e->e.getValue().getHits() == e.getValue().getValues().size() ).map(e->e.getKey())
				.collect(Collectors.toSet());
		
		
		// Removing if only low keys
		stats.entrySet().stream().forEach(e-> {
			
			 Integer max = e.getValue().getValues().values().stream().max(Integer::compare).orElse(0);
			 // TODO(p3,conf) : From conf
			if (max < 5) {
				toRemove.add(e.getKey());
			}
			
			// Deleting the one with "1" unique value
			Set<String> valToDelete = e.getValue().getValues().entrySet().stream().filter(e1-> e1.getValue().intValue() == 1).map(e1-> e1.getKey()).collect(Collectors.toSet());
			valToDelete.forEach(td -> {
				e.getValue().getValues().remove(td);
			});
			
			
		});
		
		toRemove.forEach(e-> {
			stats.remove(e);
		});
		
	}
	
	
	
	
}
