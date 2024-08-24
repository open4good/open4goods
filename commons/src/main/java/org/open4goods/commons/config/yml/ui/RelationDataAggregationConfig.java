package org.open4goods.commons.config.yml.ui;

import java.util.HashSet;
import java.util.Set;

import org.open4goods.commons.model.data.RatingType;
public class RelationDataAggregationConfig {



	/**
	 * The ratings that must be included
	 */
	private Set<RatingType> ratingsToInclude = new HashSet<>();

	/**
	 * If true, the descriptions will be added
	 */
	private Boolean includeDescriptions = true;

	public Set<RatingType> getRatingsToInclude() {
		return ratingsToInclude;
	}

	public void setRatingsToInclude(final Set<RatingType> ratingsToInclude) {
		this.ratingsToInclude = ratingsToInclude;
	}

	public Boolean getIncludeDescriptions() {
		return includeDescriptions;
	}

	public void setIncludeDescriptions(final Boolean includeDescriptions) {
		this.includeDescriptions = includeDescriptions;
	}



}
