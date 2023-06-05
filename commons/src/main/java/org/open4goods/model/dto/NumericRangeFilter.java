package org.open4goods.model.dto;

public class NumericRangeFilter {

	private String attribute;
	private Double minValue;
	private Double maxValue;



	public NumericRangeFilter() {
		super();
	}
	public NumericRangeFilter(String attribute, Double minValue, Double maxValue) {
		super();
		this.attribute = attribute;
		this.minValue = minValue;
		this.maxValue = maxValue;
	}
	public String getAttribute() {
		return attribute;
	}
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
	public Double getMinValue() {
		return minValue;
	}
	public void setMinValue(Double minValue) {
		this.minValue = minValue;
	}
	public Double getMaxValue() {
		return maxValue;
	}
	public void setMaxValue(Double maxValue) {
		this.maxValue = maxValue;
	}



}
