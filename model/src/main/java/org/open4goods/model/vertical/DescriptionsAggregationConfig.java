package org.open4goods.model.vertical;

public record DescriptionsAggregationConfig(int descriptionsTruncationLength, String descriptionsTruncationSuffix) {

        public DescriptionsAggregationConfig() {
                this(1000, "...");
        }

	/**
	 * The number of characters the description will be truncated
	 */
        

	/**
	 * The truncation suffix that will be happened if truncaation occurs
	 */
        

        public int getDescriptionsTruncationLength() {
                return descriptionsTruncationLength;
        }

        public String getDescriptionsTruncationSuffix() {
                return descriptionsTruncationSuffix;
        }



}
