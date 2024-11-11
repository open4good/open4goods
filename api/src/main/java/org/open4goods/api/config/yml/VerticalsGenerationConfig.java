package org.open4goods.api.config.yml;

import java.util.HashSet;
import java.util.Set;

public class VerticalsGenerationConfig {

	/**
	 * Max number of items to process
	 */
	private Integer limit;
	
	/**
	 * Used to fast load mappings
	 */
	private String mappingFilePath = "/opt/open4goods/config/categories-comappings.json";
	
	/**
	 * Only products for which those attributes are not empty will be processed
	 */
	private Set<String> mustExistsFields = new HashSet<>();

	/**
	 * The minimum percent of product covrage an associated category must have to be conservated
	 */
	private Double associatedCatgoriesEvictionPercent = 0.05;
	
	/**
	 * The minimum total hits a category must have
	 */
	private Integer minimumTotalHits = 1;
	
	
	public String getMappingFilePath() {
		return mappingFilePath;
	}

	public void setMappingFilePath(String mappingFilePath) {
		this.mappingFilePath = mappingFilePath;
	}

	public Set<String> getMustExistsFields() {
		return mustExistsFields;
	}

	public void setMustExistsFields(Set<String> attributesFilters) {
		this.mustExistsFields = attributesFilters;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public Double getAssociatedCatgoriesEvictionPercent() {
		return associatedCatgoriesEvictionPercent;
	}

	public void setAssociatedCatgoriesEvictionPercent(Double associatedCatgoriesEvictionPercent) {
		this.associatedCatgoriesEvictionPercent = associatedCatgoriesEvictionPercent;
	}

	public Integer getMinimumTotalHits() {
		return minimumTotalHits;
	}

	public void setMinimumTotalHits(Integer minimumTotalHits) {
		this.minimumTotalHits = minimumTotalHits;
	}

	
	
	
	
}
