package org.open4goods.commons.config.yml.datasource;

import org.open4goods.model.rating.RatingType;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

@Validated
public class CsvRatingConfig {

	@NotEmpty
	private RatingType type;

	private Integer minValue = 0;

	@NotEmpty
	private Double maxValue;


	@NotEmpty
	/**
	 * CSV colomn name where is the value
	 */
	private String value;

	public RatingType getType() {
		return type;
	}

	public void setType(final RatingType type) {
		this.type = type;
	}

	public Integer getMinValue() {
		return minValue;
	}

	public void setMinValue(final Integer minValue) {
		this.minValue = minValue;
	}


	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}


	public void setMaxValue(final Double maxValue) {
		this.maxValue = maxValue;
	}

	public Double getMaxValue() {
		return maxValue;
	}


}
