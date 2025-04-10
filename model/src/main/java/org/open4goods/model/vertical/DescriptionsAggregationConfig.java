package org.open4goods.model.vertical;

public class DescriptionsAggregationConfig {

	/**
	 * The number of characters the description will be truncated
	 */
	private int descriptionsTruncationLength = 1000;

	/**
	 * The truncation suffix that will be happened if truncaation occurs
	 */
	private String descriptionsTruncationSuffix= "...";

	public int getDescriptionsTruncationLength() {
		return descriptionsTruncationLength;
	}

	public void setDescriptionsTruncationLength(final int descriptionsTruncationLength) {
		this.descriptionsTruncationLength = descriptionsTruncationLength;
	}

	public String getDescriptionsTruncationSuffix() {
		return descriptionsTruncationSuffix;
	}

	public void setDescriptionsTruncationSuffix(final String descriptionsTruncationSuffix) {
		this.descriptionsTruncationSuffix = descriptionsTruncationSuffix;
	}



}
