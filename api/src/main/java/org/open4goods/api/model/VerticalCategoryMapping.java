package org.open4goods.api.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class VerticalCategoryMapping {

	/**
	 * Number of products that have an association for this mapping
	 */
	private Long totalHits = 0L;
	
	
	// State counters for VerticalCategories evictions handlings
	
	private Boolean keep = false;
	private Boolean toDelete = false;

	/**
	 * The associated categories encountered, with associated hit count
	 */
	private Map<String, Long> associatedCategories = new HashMap<String, Long>();

	public void updateStats(String actualCategory, Collection<String> allCategories) {
		
		
		totalHits++;

		// We increment for all except the category we are working on
		allCategories.stream()
	    .filter(e -> !e.equals(actualCategory))
	    .forEach(category -> associatedCategories.compute(category, (k, v) -> v == null ? 1L : v + 1));

	}

	
	@Override
	public String toString() {
		return totalHits +  " hits, " + associatedCategories.size() + " categories" ;
	
	}
	public Long getTotalHits() {
		return totalHits;
	}

	public void setTotalHits(Long totalHits) {
		this.totalHits = totalHits;
	}

	public Map<String, Long> getAssociatedCategories() {
		return associatedCategories;
	}

	public void setAssociatedCategories(Map<String, Long> associatedCategories) {
		this.associatedCategories = associatedCategories;
	}


	public Boolean getKeep() {
		return keep;
	}


	public void setKeep(Boolean keep) {
		this.keep = keep;
	}


	public Boolean getToDelete() {
		return toDelete;
	}


	public void setToDelete(Boolean toDelete) {
		this.toDelete = toDelete;
	}

	
	
	
	
	
	
}
