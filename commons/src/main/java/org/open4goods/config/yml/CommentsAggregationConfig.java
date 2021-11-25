package org.open4goods.config.yml;

import java.util.Set;

import com.google.common.collect.Sets;

public class CommentsAggregationConfig {
	
	
	/**
	 * List of pos categories to include in comments tagclouds
	 */
	private Set<String> posCategoriesToInclude = Sets.newHashSet("ADJ");
	
	/**
	 * Minimum number of tokens required to generate tag cloud
	 */
	private Integer minimumWordsForTagcloud = 20;
	
	

	public Set<String> getPosCategoriesToInclude() {
		return posCategoriesToInclude;
	}

	public void setPosCategoriesToInclude(Set<String> posCategoriesToInclude) {
		this.posCategoriesToInclude = posCategoriesToInclude;
	}

	public Integer getMinimumWordsForTagcloud() {
		return minimumWordsForTagcloud;
	}

	public void setMinimumWordsForTagcloud(Integer minimumWordsForTagcloud) {
		this.minimumWordsForTagcloud = minimumWordsForTagcloud;
	}
	
	
	

}
