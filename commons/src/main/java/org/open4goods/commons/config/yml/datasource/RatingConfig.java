package org.open4goods.commons.config.yml.datasource;

public class RatingConfig {

	/**
	 * Expression or Numeric
	 */
	private String min;

	/**
	 * Expression or Numeric
	 */
	private String max;

	/**
	 * Expression or Numeric
	 */
	private String voters;

	/**
	 * Expression
	 */
	private String value;

	public String getMin() {
		return min;
	}

	public void setMin(final String min) {
		this.min = min;
	}

	public String getMax() {
		return max;
	}

	public void setMax(final String max) {
		this.max = max;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public String getVoters() {
		return voters;
	}

	public void setVoters(final String voters) {
		this.voters = voters;
	}

}
