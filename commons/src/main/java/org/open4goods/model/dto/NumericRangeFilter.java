package org.open4goods.model.dto;

public class NumericRangeFilter {

	private String key;
	private Double minValue;
	private Double maxValue;



	public NumericRangeFilter() {
		super();
	}
	public NumericRangeFilter(String attribute, Double minValue, Double maxValue) {
		super();
		this.key = attribute;
		this.minValue = minValue;
		this.maxValue = maxValue;
	}
	
	@Override
	public String toString() {
	
		return key+ " : " + minValue +"<>" + maxValue;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NumericRangeFilter) {
			return this.toString().equals(obj.toString());
		}
		return false;
	}
	
	
	public String getKey() {
		return key;
	}
	public void setKey(String attribute) {
		this.key = attribute;
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
