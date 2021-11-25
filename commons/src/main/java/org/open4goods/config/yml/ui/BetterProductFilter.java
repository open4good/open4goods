package org.open4goods.config.yml.ui;

public class BetterProductFilter {

	private String name;

	private FilterClause filterClause;

	private Boolean required = false;

	private Boolean excludeVirtualRatings = true;

	public String getName() {
		return name;
	}

	public void setName(final String attributeName) {
		name = attributeName;
	}

	public FilterClause getFilterClause() {
		return filterClause;
	}

	public void setFilterClause(final FilterClause filterClause) {
		this.filterClause = filterClause;
	}

	public Boolean getRequired() {
		return required;
	}

	public void setRequired(final Boolean required) {
		this.required = required;
	}

	public Boolean getExcludeVirtualRatings() {
		return excludeVirtualRatings;
	}

	public void setExcludeVirtualRatings(final Boolean excludeVirtualRatings) {
		this.excludeVirtualRatings = excludeVirtualRatings;
	}



}
